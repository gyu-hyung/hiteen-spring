# Kubernetes 클러스터 설정 가이드

운영 환경을 위한 Kubernetes 클러스터 구축 가이드입니다.

## 📋 목차

1. [사전 요구사항](#-사전-요구사항)
2. [서버 기본 설정 (모든 노드 공통)](#-서버-기본-설정-모든-노드-공통)
3. [마스터 노드 설정](#-마스터-노드-설정)
4. [워커 노드 설정](#-워커-노드-설정)
5. [클러스터 검증](#-클러스터-검증)
6. [필수 컴포넌트 설치](#-필수-컴포넌트-설치)
7. [NFS 서버 설정](#-nfs-서버-설정)
8. [트러블슈팅](#-트러블슈팅)

---

## 📌 사전 요구사항

### 권장 서버 스펙

| 역할 | CPU | RAM | 디스크 | 수량 |
|------|-----|-----|--------|------|
| Master Node | 4+ cores | 8GB+ | 100GB SSD | 1~3 (HA) |
| Worker Node | 4+ cores | 16GB+ | 200GB SSD | 2+ |
| NFS Server | 2+ cores | 4GB+ | 500GB+ HDD/SSD | 1 |

### 네트워크 요구사항

| 포트 | 프로토콜 | 용도 | 노드 |
|------|----------|------|------|
| 6443 | TCP | Kubernetes API Server | Master |
| 2379-2380 | TCP | etcd | Master |
| 10250 | TCP | Kubelet API | All |
| 10259 | TCP | kube-scheduler | Master |
| 10257 | TCP | kube-controller-manager | Master |
| 30000-32767 | TCP | NodePort Services | Worker |
| 179 | TCP | Calico BGP | All |
| 4789 | UDP | Calico VXLAN | All |

### OS 요구사항

- **권장 OS**: Rocky Linux 9 / AlmaLinux 9 / Ubuntu 22.04 LTS
- **커널 버전**: 5.x 이상
- **SELinux**: permissive 또는 disabled (선택)

---

## 🔧 서버 기본 설정 (모든 노드 공통)

> ⚠️ **아래 설정은 마스터/워커 모든 노드에서 실행해야 합니다.**

### 1. 시스템 업데이트 및 필수 패키지 설치

```bash
# 시스템 업데이트
dnf update -y

# 필수 패키지 설치
dnf install -y \
    curl \
    wget \
    vim \
    git \
    net-tools \
    bind-utils \
    bash-completion \
    yum-utils \
    device-mapper-persistent-data \
    lvm2 \
    iproute-tc
```

### 2. 호스트명 설정

```bash
# 마스터 노드
hostnamectl set-hostname k8s-master-1

# 워커 노드 1
hostnamectl set-hostname k8s-worker-1

# 워커 노드 2
hostnamectl set-hostname k8s-worker-2
```

### 3. /etc/hosts 설정

```bash
cat >> /etc/hosts << EOF
# Kubernetes Cluster - Production
10.8.0.100  k8s-master-1
10.8.0.101  k8s-worker-1
10.8.0.102  k8s-worker-2
10.8.0.200  nfs-server
EOF
```

### 4. Swap 비활성화

```bash
# 즉시 비활성화
swapoff -a

# 영구 비활성화 (재부팅 후에도 적용)
sed -i '/swap/d' /etc/fstab

# 확인
free -h
```

### 5. 방화벽 설정

```bash
# 방화벽 비활성화 (테스트 환경)
systemctl stop firewalld
systemctl disable firewalld

# 또는 필요한 포트만 개방 (운영 환경 권장)
# firewall-cmd --permanent --add-port=6443/tcp
# firewall-cmd --permanent --add-port=2379-2380/tcp
# firewall-cmd --permanent --add-port=10250/tcp
# firewall-cmd --permanent --add-port=10259/tcp
# firewall-cmd --permanent --add-port=10257/tcp
# firewall-cmd --permanent --add-port=30000-32767/tcp
# firewall-cmd --permanent --add-port=179/tcp
# firewall-cmd --permanent --add-port=4789/udp
# firewall-cmd --reload
```

### 6. SELinux 설정

```bash
# permissive 모드로 변경 (권장)
setenforce 0
sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config

# 또는 완전 비활성화
# sed -i 's/^SELINUX=enforcing$/SELINUX=disabled/' /etc/selinux/config
```

### 7. 커널 모듈 및 네트워크 설정

```bash
# 필수 커널 모듈 로드
cat <<EOF | tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

modprobe overlay
modprobe br_netfilter

# 커널 파라미터 설정
cat <<EOF | tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

# 적용
sysctl --system

# 확인
lsmod | grep br_netfilter
lsmod | grep overlay
sysctl net.bridge.bridge-nf-call-iptables net.bridge.bridge-nf-call-ip6tables net.ipv4.ip_forward
```

### 8. Containerd 설치 (컨테이너 런타임)

```bash
# Docker CE 레포지토리 추가
dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# Containerd 설치
dnf install -y containerd.io

# 기본 설정 파일 생성
mkdir -p /etc/containerd
containerd config default | tee /etc/containerd/config.toml > /dev/null

# SystemdCgroup 활성화 (중요!)
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

# 서비스 시작 및 활성화
systemctl enable --now containerd

# 상태 확인
systemctl status containerd
```

### 9. Kubernetes 패키지 설치

```bash
# Kubernetes 레포지토리 추가 (v1.29)
cat <<EOF | tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://pkgs.k8s.io/core:/stable:/v1.29/rpm/
enabled=1
gpgcheck=1
gpgkey=https://pkgs.k8s.io/core:/stable:/v1.29/rpm/repodata/repomd.xml.key
exclude=kubelet kubeadm kubectl cri-tools kubernetes-cni
EOF

# 패키지 설치
dnf install -y kubelet kubeadm kubectl --disableexcludes=kubernetes

# kubelet 활성화
systemctl enable kubelet

# 버전 확인
kubeadm version
kubectl version --client
```

### 10. crictl 설정

```bash
# crictl이 containerd를 사용하도록 설정
cat <<EOF | tee /etc/crictl.yaml
runtime-endpoint: unix:///run/containerd/containerd.sock
image-endpoint: unix:///run/containerd/containerd.sock
timeout: 10
debug: false
EOF
```

---

## 👑 마스터 노드 설정

> 마스터 노드에서만 실행하는 설정입니다.

### 1. 클러스터 초기화

```bash
# 클러스터 초기화 (Pod 네트워크 CIDR: Calico 기본값)
kubeadm init \
  --pod-network-cidr=192.168.0.0/16 \
  --apiserver-advertise-address=<MASTER_INTERNAL_IP> \
  --control-plane-endpoint=<MASTER_INTERNAL_IP>:6443

# 예시:
# kubeadm init \
#   --pod-network-cidr=192.168.0.0/16 \
#   --apiserver-advertise-address=10.8.0.100 \
#   --control-plane-endpoint=10.8.0.100:6443
```

> ⚠️ **중요**: 초기화 완료 후 출력되는 `kubeadm join` 명령어를 반드시 저장하세요!

### 2. kubectl 설정

```bash
# root 사용자
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config

# 또는 환경변수로 설정
export KUBECONFIG=/etc/kubernetes/admin.conf
echo 'export KUBECONFIG=/etc/kubernetes/admin.conf' >> ~/.bashrc

# 자동완성 설정
kubectl completion bash | tee /etc/bash_completion.d/kubectl > /dev/null
source /etc/bash_completion.d/kubectl
```

### 3. Calico CNI 설치

```bash
# Calico 설치
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.27.3/manifests/calico.yaml

# 설치 확인 (모든 Pod이 Running 상태가 될 때까지 대기)
watch kubectl get pods -n kube-system

# calico-node, calico-kube-controllers가 Running이면 성공
```

### 4. 마스터 노드 상태 확인

```bash
# 노드 상태 확인
kubectl get nodes

# 시스템 Pod 상태 확인
kubectl get pods -n kube-system

# 클러스터 정보 확인
kubectl cluster-info
```

### 5. 노드 라벨링

```bash
# 마스터 노드에 라벨 추가
kubectl label node k8s-master-1 node-type=master

# 워커 노드에 라벨 추가 (워커 조인 후)
kubectl label node k8s-worker-1 node-type=private
kubectl label node k8s-worker-2 node-type=private
```

### 6. Join 토큰 재생성 (필요 시)

```bash
# 기존 토큰 만료 시 새 토큰 생성
kubeadm token create --print-join-command
```

---

## 🖥️ 워커 노드 설정

> [서버 기본 설정](#-서버-기본-설정-모든-노드-공통)을 먼저 완료한 후 진행하세요.

### 1. 클러스터 조인

```bash
# 마스터에서 출력된 join 명령어 실행
kubeadm join <MASTER_IP>:6443 \
  --token <TOKEN> \
  --discovery-token-ca-cert-hash sha256:<HASH>

# 예시:
# kubeadm join 10.8.0.100:6443 \
#   --token abcdef.0123456789abcdef \
#   --discovery-token-ca-cert-hash sha256:1234567890abcdef...
```

### 2. 조인 확인 (마스터에서)

```bash
# 마스터 노드에서 실행
kubectl get nodes -o wide

# 출력 예시:
# NAME           STATUS   ROLES           AGE   VERSION   INTERNAL-IP
# k8s-master-1   Ready    control-plane   10m   v1.29.x   10.8.0.100
# k8s-worker-1   Ready    <none>          2m    v1.29.x   10.8.0.101
# k8s-worker-2   Ready    <none>          1m    v1.29.x   10.8.0.102
```

---

## ✅ 클러스터 검증

### 1. 노드 상태 확인

```bash
kubectl get nodes -o wide
kubectl describe nodes
```

### 2. 시스템 컴포넌트 확인

```bash
kubectl get pods -n kube-system
kubectl get componentstatuses  # deprecated but still works
```

### 3. 테스트 Pod 배포

```bash
# nginx 테스트 배포
kubectl create deployment nginx-test --image=nginx
kubectl expose deployment nginx-test --port=80 --type=NodePort

# 확인
kubectl get pods -o wide
kubectl get svc nginx-test

# 정리
kubectl delete deployment nginx-test
kubectl delete svc nginx-test
```

### 4. DNS 테스트

```bash
kubectl run dns-test --image=busybox:1.28 --rm -it --restart=Never -- nslookup kubernetes

# 출력 예시:
# Server:    10.96.0.10
# Address 1: 10.96.0.10 kube-dns.kube-system.svc.cluster.local
# Name:      kubernetes
# Address 1: 10.96.0.1 kubernetes.default.svc.cluster.local
```

---

## 📦 필수 컴포넌트 설치

### 1. local-path-provisioner (동적 PV)

```bash
# 네임스페이스 생성
kubectl create ns local-path-storage

# local-path-provisioner 설치
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml

# 확인
kubectl get pods -n local-path-storage
kubectl get storageclass
```

### 2. Ingress Nginx Controller

```bash
# 네임스페이스 생성
kubectl create namespace ingress-nginx

# Bare Metal용 ingress-nginx 설치
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/baremetal/deploy.yaml

# 확인
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

# NodePort 확인 (외부 접근용)
kubectl get svc ingress-nginx-controller -n ingress-nginx
```

### 3. Metrics Server (HPA용)

```bash
# Metrics Server 설치
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# TLS 검증 비활성화 (Self-signed 인증서 환경)
kubectl patch deployment metrics-server -n kube-system --type='json' -p='[
  {
    "op": "add",
    "path": "/spec/template/spec/containers/0/args/-",
    "value": "--kubelet-insecure-tls"
  }
]'

# 확인
kubectl top nodes
kubectl top pods -A
```

### 4. Helm 설치 (마스터 노드)

```bash
# Helm 설치
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# 확인
helm version

# 자동완성 설정
helm completion bash > /etc/bash_completion.d/helm
source /etc/bash_completion.d/helm
```

---

## 💾 NFS 서버 설정

> NFS 서버용 별도 서버에서 실행합니다.

### 1. NFS 서버 설치

```bash
# NFS 패키지 설치
dnf install -y nfs-utils

# NFS 서비스 활성화
systemctl enable --now nfs-server rpcbind

# 상태 확인
systemctl status nfs-server
```

### 2. 공유 디렉토리 생성

```bash
# Assets 저장소
mkdir -p /srv/nfs/assets
chmod 777 /srv/nfs/assets

# Backup 저장소
mkdir -p /srv/nfs/backup
chmod 777 /srv/nfs/backup
```

### 3. exports 설정

```bash
cat >> /etc/exports << EOF
# Kubernetes NFS Shares
/srv/nfs/assets    10.8.0.0/24(rw,sync,no_subtree_check,no_root_squash)
/srv/nfs/backup    10.8.0.0/24(rw,sync,no_subtree_check,no_root_squash)
EOF

# 설정 적용
exportfs -rav

# 확인
exportfs -v
showmount -e localhost
```

### 4. 방화벽 설정

```bash
firewall-cmd --permanent --add-service=nfs
firewall-cmd --permanent --add-service=rpc-bind
firewall-cmd --permanent --add-service=mountd
firewall-cmd --reload
```

### 5. 워커 노드에서 NFS 클라이언트 설치

```bash
# 모든 워커 노드에서 실행
dnf install -y nfs-utils

# 마운트 테스트
mkdir -p /mnt/nfs-test
mount -t nfs 10.8.0.200:/srv/nfs/assets /mnt/nfs-test
ls /mnt/nfs-test
umount /mnt/nfs-test
```

---

## 🔧 트러블슈팅

### 노드가 NotReady 상태일 때

```bash
# 노드 상태 확인
kubectl describe node <node-name>

# kubelet 로그 확인
journalctl -u kubelet -f

# containerd 상태 확인
systemctl status containerd
```

### Pod이 Pending 상태일 때

```bash
# Pod 상태 확인
kubectl describe pod <pod-name> -n <namespace>

# 이벤트 확인
kubectl get events -n <namespace> --sort-by='.lastTimestamp'
```

### 네트워크 문제

```bash
# Calico Pod 상태 확인
kubectl get pods -n kube-system -l k8s-app=calico-node

# Calico 로그 확인
kubectl logs -n kube-system -l k8s-app=calico-node

# CoreDNS 확인
kubectl get pods -n kube-system -l k8s-app=kube-dns
```

### 클러스터 리셋 (완전 초기화)

```bash
# 워커 노드에서
kubeadm reset -f
rm -rf /etc/cni/net.d
rm -rf $HOME/.kube
iptables -F && iptables -t nat -F && iptables -t mangle -F && iptables -X

# 마스터 노드에서
kubeadm reset -f
rm -rf /etc/cni/net.d
rm -rf $HOME/.kube
rm -rf /var/lib/etcd
iptables -F && iptables -t nat -F && iptables -t mangle -F && iptables -X
```

### Join 토큰 만료 시

```bash
# 마스터에서 새 토큰 생성
kubeadm token create --print-join-command
```

---

## 📝 빠른 참조 명령어

```bash
# 노드 상태
kubectl get nodes -o wide

# 모든 Pod 상태
kubectl get pods -A

# 특정 네임스페이스 리소스
kubectl get all -n <namespace>

# 로그 확인
kubectl logs <pod-name> -n <namespace> -f

# Pod 접속
kubectl exec -it <pod-name> -n <namespace> -- /bin/bash

# 리소스 사용량
kubectl top nodes
kubectl top pods -A

# 이벤트 확인
kubectl get events -A --sort-by='.lastTimestamp'
```

---

## 📚 다음 단계

1. [운영 배포 가이드](./PRODUCTION-DEPLOY.md) - 인프라 및 애플리케이션 배포
2. 모니터링 설정 (Prometheus + Grafana)
3. 백업 정책 수립
4. CI/CD 파이프라인 구축

