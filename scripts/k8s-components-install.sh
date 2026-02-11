#!/bin/bash
# ============================================
# Kubernetes 필수 컴포넌트 설치 스크립트
# 마스터 노드에서 실행
# ============================================

set -e

echo "=========================================="
echo "📦 Kubernetes 필수 컴포넌트 설치 스크립트"
echo "=========================================="

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# kubectl 연결 확인
log_info "클러스터 연결 확인 중..."
if ! kubectl get nodes &> /dev/null; then
    log_error "클러스터에 연결할 수 없습니다. kubeconfig를 확인하세요."
    exit 1
fi

# 노드가 Ready 상태인지 확인
NODE_STATUS=$(kubectl get nodes -o jsonpath='{.items[0].status.conditions[?(@.type=="Ready")].status}')
if [ "$NODE_STATUS" != "True" ]; then
    log_warn "노드가 아직 Ready 상태가 아닙니다. 잠시 대기 후 다시 시도하세요."
    kubectl get nodes
    exit 1
fi

log_info "클러스터 연결 확인 완료!"
kubectl get nodes

# ==========================================
# 1. local-path-provisioner
# ==========================================
log_info "local-path-provisioner 설치 중..."
kubectl create ns local-path-storage || true
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml

echo "  대기 중 (10초)..."
sleep 10
kubectl get pods -n local-path-storage

# ==========================================
# 2. Ingress Nginx
# ==========================================
log_info "Ingress Nginx Controller 설치 중..."
kubectl create namespace ingress-nginx || true
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/baremetal/deploy.yaml

echo "  대기 중 (30초)..."
sleep 30
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

# ==========================================
# 3. Metrics Server
# ==========================================
log_info "Metrics Server 설치 중..."
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# TLS 검증 비활성화 패치
kubectl patch deployment metrics-server -n kube-system --type='json' -p='[
  {
    "op": "add",
    "path": "/spec/template/spec/containers/0/args/-",
    "value": "--kubelet-insecure-tls"
  }
]' || true

echo "  대기 중 (20초)..."
sleep 20
kubectl get pods -n kube-system | grep metrics-server

# ==========================================
# 4. Helm 설치
# ==========================================
log_info "Helm 설치 중..."
if ! command -v helm &> /dev/null; then
    curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
    helm completion bash > /etc/bash_completion.d/helm
fi
helm version

# ==========================================
# 완료
# ==========================================
echo ""
echo "=========================================="
echo -e "${GREEN}✅ 필수 컴포넌트 설치 완료!${NC}"
echo "=========================================="
echo ""
echo "설치된 컴포넌트:"
echo "  ✓ local-path-provisioner"
echo "  ✓ ingress-nginx"
echo "  ✓ metrics-server"
echo "  ✓ helm"
echo ""
echo "StorageClass 확인:"
kubectl get storageclass
echo ""
echo "Ingress NodePort 확인:"
kubectl get svc -n ingress-nginx ingress-nginx-controller
echo ""
echo "=========================================="
echo "다음 단계:"
echo "=========================================="
echo ""
echo "  1. 워커 노드 조인 (워커 노드에서 실행):"
echo "     cat /root/k8s-join-command.txt"
echo ""
echo "  2. 인프라 배포:"



echo "     helm upgrade --install hiteen-infra ./hiteen-infra-chart -n hiteen --create-namespace"
echo ""
echo "  3. 앱 배포:"
echo "     helm upgrade --install hiteen-app ./hiteen-app-chart -n hiteen"
echo ""
