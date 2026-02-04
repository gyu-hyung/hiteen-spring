#!/bin/bash
# ============================================
# Kubernetes 노드 초기화 스크립트
# Rocky Linux 9 / AlmaLinux 9 / CentOS Stream 9
# ============================================

set -e

echo "=========================================="
echo "🚀 Kubernetes 노드 초기화 스크립트"
echo "=========================================="


# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 로그 함수
log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Root 권한 확인
if [ "$EUID" -ne 0 ]; then
    log_error "이 스크립트는 root 권한으로 실행해야 합니다."
    exit 1
fi

# 변수 설정
K8S_VERSION="1.34"
CONTAINERD_VERSION="latest"

# ==========================================
# 1. 시스템 업데이트
# ==========================================
log_info "시스템 업데이트 중..."

# 불필요한 Node.js 제거 (K8s에 불필요하고 충돌 발생 가능)
if rpm -q nodejs &> /dev/null; then
    log_warn "Node.js 충돌 방지를 위해 기존 Node.js 제거 중..."
    dnf remove -y nodejs nodejs-full-i18n npm || true
fi

# nodesource 레포 비활성화 (있는 경우)
if [ -f /etc/yum.repos.d/nodesource*.repo ]; then
    log_warn "nodesource 레포 비활성화 중..."
    sed -i 's/enabled=1/enabled=0/g' /etc/yum.repos.d/nodesource*.repo || true
fi

dnf update -y --skip-broken || dnf update -y --allowerasing || true

# ==========================================
# 2. 필수 패키지 설치
# ==========================================
log_info "필수 패키지 설치 중..."
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

# ==========================================
# 3. Swap 비활성화
# ==========================================
log_info "Swap 비활성화 중..."
swapoff -a
sed -i '/swap/d' /etc/fstab
log_info "Swap 상태: $(free -h | grep Swap)"

# ==========================================
# 4. SELinux 설정
# ==========================================
log_info "SELinux를 permissive 모드로 설정 중..."
setenforce 0 || true
sed -i 's/^SELINUX=enforcing$/SELINUX=permissive/' /etc/selinux/config

# ==========================================
# 5. 방화벽 비활성화
# ==========================================
log_info "방화벽 비활성화 중..."
systemctl stop firewalld || true
systemctl disable firewalld || true

# ==========================================
# 6. 커널 모듈 설정
# ==========================================
log_info "커널 모듈 설정 중..."
cat <<EOF | tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

modprobe overlay
modprobe br_netfilter

# ==========================================
# 7. 커널 파라미터 설정
# ==========================================
log_info "커널 파라미터 설정 중..."
cat <<EOF | tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sysctl --system

# ==========================================
# 8. Containerd 설치
# ==========================================
log_info "Containerd 설치 중..."
dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
dnf install -y containerd.io

mkdir -p /etc/containerd
containerd config default | tee /etc/containerd/config.toml > /dev/null
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml

systemctl enable --now containerd
log_info "Containerd 상태: $(systemctl is-active containerd)"

# ==========================================
# 9. Kubernetes 설치
# ==========================================
log_info "Kubernetes v${K8S_VERSION} 설치 중..."
cat <<EOF | tee /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://pkgs.k8s.io/core:/stable:/v${K8S_VERSION}/rpm/
enabled=1
gpgcheck=1
gpgkey=https://pkgs.k8s.io/core:/stable:/v${K8S_VERSION}/rpm/repodata/repomd.xml.key
exclude=kubelet kubeadm kubectl cri-tools kubernetes-cni
EOF

dnf install -y kubelet kubeadm kubectl --disableexcludes=kubernetes
systemctl enable kubelet

# ==========================================
# 10. crictl 설정
# ==========================================
log_info "crictl 설정 중..."
cat <<EOF | tee /etc/crictl.yaml
runtime-endpoint: unix:///run/containerd/containerd.sock
image-endpoint: unix:///run/containerd/containerd.sock
timeout: 10
debug: false
EOF

# ==========================================
# 완료
# ==========================================
echo ""
echo "=========================================="
echo -e "${GREEN}✅ 노드 초기화 완료!${NC}"
echo "=========================================="
echo ""
echo "설치된 버전:"
echo "  - kubeadm: $(kubeadm version -o short)"
echo "  - kubectl: $(kubectl version --client -o yaml | grep gitVersion | head -1)"
echo "  - containerd: $(containerd --version)"
echo ""
echo "=========================================="
echo "다음 단계:"
echo "=========================================="
echo ""
echo "  마스터 노드인 경우:"
echo "    ./k8s-master-setup.sh"
echo ""
echo "  워커 노드인 경우:"
echo "    kubeadm join <master-ip>:6443 --token <token> --discovery-token-ca-cert-hash sha256:<hash>"
echo ""
echo "  (마스터 노드의 /root/k8s-join-command.txt 파일 참조)"
echo ""
