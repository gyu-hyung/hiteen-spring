# Kubernetes 클러스터 설정 가이드 (Ubuntu 22.04)

Ubuntu 22.04 LTS 환경을 위한 Kubernetes 클러스터 구축 가이드입니다.

---

## 🔧 서버 기본 설정 (모든 노드 공통)

### 1. 시스템 업데이트 및 필수 패키지 설치

```bash
# 시스템 업데이트
apt update && apt upgrade -y

# 필수 패키지 설치
apt install -y \
    curl \
    wget \
    vim \
    git \
    net-tools \
    dnsutils \
    bash-completion \
    apt-transport-https \
    ca-certificates \
    gnupg \
    lsb-release \
    software-properties-common
```

### 2. 호스트명 설정

```bash
# 마스터 노드
hostnamectl set-hostname k8s-master-1

# 워커 노드
hostnamectl set-hostname k8s-worker-1
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

# 영구 비활성화
sed -i '/swap/s/^/#/' /etc/fstab

# 확인
free -h
```

### 5. 방화벽 설정

```bash
# UFW 비활성화 (테스트 환경)
ufw disable

# 또는 필요한 포트만 개방 (운영 환경)
# ufw allow 6443/tcp
# ufw allow 2379:2380/tcp
# ufw allow 10250/tcp
# ufw allow 10259/tcp
# ufw allow 10257/tcp
# ufw allow 30000:32767/tcp
# ufw enable
```

### 6. 커널 모듈 및 네트워크 설정

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
```

### 7. Containerd 설치

```bash
# Docker 공식 GPG 키 추가
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

# Docker 레포지토리 추가
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

# Containerd 설치
apt update
apt install -y containerd.io

# 기본 설정 파일 생성
mkdir -p /etc/containerd
containerd config default | tee /etc/containerd/config.toml > /dev/null

# SystemdCgroup 활성화
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

# 서비스 재시작
systemctl restart containerd
systemctl enable containerd
```

### 8. Kubernetes 패키지 설치

```bash
# Kubernetes GPG 키 추가
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key | gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

# Kubernetes 레포지토리 추가
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.29/deb/ /' | tee /etc/apt/sources.list.d/kubernetes.list

# 패키지 설치
apt update
apt install -y kubelet kubeadm kubectl

# 버전 고정 (자동 업데이트 방지)
apt-mark hold kubelet kubeadm kubectl

# kubelet 활성화
systemctl enable kubelet
```

### 9. crictl 설정

```bash
cat <<EOF | tee /etc/crictl.yaml
runtime-endpoint: unix:///run/containerd/containerd.sock
image-endpoint: unix:///run/containerd/containerd.sock
timeout: 10
debug: false
EOF
```

---

## 👑 마스터 노드 설정

### 1. 클러스터 초기화

```bash
kubeadm init \
  --pod-network-cidr=192.168.0.0/16 \
  --apiserver-advertise-address=<MASTER_INTERNAL_IP> \
  --control-plane-endpoint=<MASTER_INTERNAL_IP>:6443
```

### 2. kubectl 설정

```bash
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config

# 자동완성
echo 'source <(kubectl completion bash)' >> ~/.bashrc
echo 'alias k=kubectl' >> ~/.bashrc
echo 'complete -o default -F __start_kubectl k' >> ~/.bashrc
source ~/.bashrc
```

### 3. Calico CNI 설치

```bash
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.27.3/manifests/calico.yaml
```

---

## 🖥️ 워커 노드 설정

```bash
# 마스터에서 출력된 join 명령어 실행
kubeadm join <MASTER_IP>:6443 \
  --token <TOKEN> \
  --discovery-token-ca-cert-hash sha256:<HASH>
```

---

## 💾 NFS 서버 설정 (Ubuntu)

```bash
# NFS 서버 설치
apt install -y nfs-kernel-server

# 디렉토리 생성
mkdir -p /srv/nfs/assets
mkdir -p /srv/nfs/backup
chmod 777 /srv/nfs/assets /srv/nfs/backup

# exports 설정
cat >> /etc/exports << EOF
/srv/nfs/assets    10.8.0.0/24(rw,sync,no_subtree_check,no_root_squash)
/srv/nfs/backup    10.8.0.0/24(rw,sync,no_subtree_check,no_root_squash)
EOF

# 적용
exportfs -rav
systemctl restart nfs-kernel-server
```

### 클라이언트 (워커 노드)

```bash
apt install -y nfs-common
```

---

자세한 내용은 [K8S-CLUSTER-SETUP.md](./K8S-CLUSTER-SETUP.md)를 참조하세요.

