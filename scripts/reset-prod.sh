#!/bin/bash
# ============================================
# 운영 서버 초기화 스크립트
# deploy-prod.sh 테스트를 위해 모든 리소스 삭제
# ============================================

set -e

NAMESPACE="hiteen"

echo "=========================================="
echo "🗑️ Hiteen 운영 서버 초기화"
echo "=========================================="
echo "Namespace: $NAMESPACE"
echo ""
echo "⚠️ 주의: 이 스크립트는 모든 리소스를 삭제합니다!"
echo ""
read -p "계속하시겠습니까? (y/N): " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
  echo "취소되었습니다."
  exit 0
fi

echo ""

# 1. Helm 릴리스 삭제
echo "[1/4] Helm 릴리스 삭제..."
helm uninstall hiteen-app -n $NAMESPACE --ignore-not-found 2>/dev/null || true
helm uninstall hiteen-infra -n $NAMESPACE --ignore-not-found 2>/dev/null || true
echo "  ✅ Helm 릴리스 삭제 완료"

# 2. PVC/PV 삭제
echo "[2/4] PVC/PV 삭제..."
kubectl delete pvc --all -n $NAMESPACE --ignore-not-found 2>/dev/null || true
kubectl delete pv nfs-pv-images-prod --ignore-not-found 2>/dev/null || true
kubectl delete pv nfs-pv-images --ignore-not-found 2>/dev/null || true
echo "  ✅ PVC/PV 삭제 완료"

# 3. Secret 삭제
echo "[3/4] Secret 삭제..."
kubectl delete secret gitlab-registry -n $NAMESPACE --ignore-not-found 2>/dev/null || true
kubectl delete secret redis-secret -n $NAMESPACE --ignore-not-found 2>/dev/null || true
kubectl delete secret firebase-secret -n $NAMESPACE --ignore-not-found 2>/dev/null || true
kubectl delete secret hiteen-app-secret -n $NAMESPACE --ignore-not-found 2>/dev/null || true
echo "  ✅ Secret 삭제 완료"

# 4. Namespace 삭제 (선택사항)
echo "[4/4] Namespace 삭제..."
kubectl delete ns $NAMESPACE --ignore-not-found 2>/dev/null || true

# Namespace가 완전히 삭제될 때까지 대기
echo "  ⏳ Namespace 삭제 대기 중..."
while kubectl get ns $NAMESPACE 2>/dev/null; do
  sleep 2
done
echo "  ✅ Namespace 삭제 완료"

echo ""
echo "=========================================="
echo "✅ 초기화 완료!"
echo "=========================================="
echo ""
echo "이제 deploy-prod.sh를 실행할 수 있습니다:"
echo "  ./deploy-prod.sh prod-0.0.1"

