#!/bin/bash
# ============================================
# Kubernetes 마스터 노드 설정 스크립트
# ============================================

set -e

echo "=========================================="
echo "👑 Kubernetes 마스터 노드 설정 스크립트"
echo "=========================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Root 권한 확인
if [ "$EUID" -ne 0 ]; then
    log_error "이 스크립트는 root 권한으로 실행해야 합니다."
    exit 1
fi

# 자동으로 내부 IP 감지
AUTO_IP=$(ip -4 addr show eth0 2>/dev/null | grep -oP '(?<=inet\s)\d+(\.\d+){3}' | head -1)
if [ -z "$AUTO_IP" ]; then
    AUTO_IP=$(ip -4 addr show | grep -oP '(?<=inet\s)10\.\d+\.\d+\.\d+' | head -1)
fi

echo ""
echo "감지된 IP: $AUTO_IP"
read -p "마스터 노드 내부 IP를 입력하세요 [$AUTO_IP]: " MASTER_IP

# 입력이 없으면 자동 감지된 IP 사용
if [ -z "$MASTER_IP" ]; then
    MASTER_IP=$AUTO_IP
fi

if [ -z "$MASTER_IP" ]; then
    log_error "IP 주소가 필요합니다."
    exit 1
fi

echo ""
log_info "사용할 IP: $MASTER_IP"

# ==========================================
# 1. 클러스터 초기화
# ==========================================
log_info "Kubernetes 클러스터 초기화 중..."
kubeadm init \
    --pod-network-cidr=192.168.0.0/16 \
    --apiserver-advertise-address=${MASTER_IP} \
    --control-plane-endpoint=${MASTER_IP}:6443 \
    | tee /root/kubeadm-init.log

# ==========================================
# 2. kubectl 설정
# ==========================================
log_info "kubectl 설정 중..."
mkdir -p $HOME/.kube
cp -f /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config

# ==========================================
# 3. kubectl 자동완성 및 alias
# ==========================================
log_info "kubectl 자동완성 및 alias 설정 중..."
kubectl completion bash | tee /etc/bash_completion.d/kubectl > /dev/null
echo 'alias k=kubectl' >> ~/.bashrc
echo 'complete -o default -F __start_kubectl k' >> ~/.bashrc

# 현재 세션에도 적용
source /etc/bash_completion.d/kubectl 2>/dev/null || true
alias k=kubectl

# ==========================================
# 4. Calico CNI 설치
# ==========================================
log_info "Calico CNI 설치 중..."
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.27.3/manifests/calico.yaml

# ==========================================
# 5. 상태 확인 (충분한 대기 시간)
# ==========================================
log_info "Calico Pod 상태 확인 (60초 대기)..."
sleep 60
kubectl get pods -n kube-system

# ==========================================
# 6. Join 명령어 저장
# ==========================================
log_info "Join 명령어 생성 중..."
kubeadm token create --print-join-command > /root/k8s-join-command.txt
JOIN_COMMAND=$(cat /root/k8s-join-command.txt)

# ==========================================
# 7. 노드 라벨링
# ==========================================
log_info "노드 라벨 설정 중..."
NODE_NAME=$(kubectl get nodes -o jsonpath='{.items[0].metadata.name}')
kubectl label node $NODE_NAME node-type=private --overwrite

echo ""
echo "=========================================="
echo -e "${GREEN}✅ 마스터 노드 설정 완료!${NC}"
echo "=========================================="
echo ""
echo "노드 상태:"
kubectl get nodes -o wide
echo ""
echo "시스템 Pod 상태:"
kubectl get pods -n kube-system
echo ""
echo "=========================================="
echo "워커 노드 Join 명령어 (저장됨: /root/k8s-join-command.txt):"
echo "=========================================="
echo ""
echo "$JOIN_COMMAND"
echo ""
echo "=========================================="
echo "다음 단계: ./k8s-components-install.sh 실행"
echo "=========================================="
echo ""
echo "kubectl 단축키 적용하려면: source ~/.bashrc"
echo ""

