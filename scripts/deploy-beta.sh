#!/bin/bash
# ============================================
# 개발(Beta) 서버 수동 배포 스크립트
# 마스터 노드에서 실행
#
# 포함 사항:
# - Namespace, Secrets 생성
# - Ingress Controller hostNetwork 설정
# - cert-manager 설치 및 Let's Encrypt 인증서 발급
# - Redis Cluster (3개) 배포
# - Spring Boot API 배포
# ============================================

set -e

# 설정
NAMESPACE="hiteen"
DOMAIN="beta-api.hiteen.kr"
EMAIL="gud5603@gmail.com"
REGISTRY="gitlab.barunsoft.net:6005"
IMAGE="gitlab.barunsoft.net:6005/jiasoft/hiteen2-server"
TAG="${1:-prod-0.0.1}"

# GitLab Registry 인증 정보
GITLAB_USER="${GITLAB_USER:-gud5603@gmail.com}"
GITLAB_TOKEN="${GITLAB_TOKEN:-glpat-Bi5zgoBLnyFfR3KUaxxm}"

# Redis 비밀번호
REDIS_PASSWORD="${REDIS_PASSWORD:-hiteen1234}"

# NFS 설정
NFS_SERVER="10.8.0.159"
NFS_PATH="/srv/nfs/assets"

echo "=========================================="
echo "🚀 Hiteen 개발(Beta) 서버 배포 시작"
echo "=========================================="
echo "Namespace: $NAMESPACE"
echo "Domain: $DOMAIN"
echo "Image: $IMAGE:$TAG"
echo "NFS Server: $NFS_SERVER"
echo "Config: values-beta.yaml (리소스 절약 모드)"
echo ""

# ============================================
# 1. Namespace 생성
# ============================================
echo "[1/12] Namespace 생성..."
kubectl get ns $NAMESPACE 2>/dev/null || kubectl create ns $NAMESPACE

# ============================================
# 2. GitLab Registry Secret 생성
# ============================================
echo "[2/12] GitLab Registry Secret 생성..."
kubectl delete secret gitlab-registry -n $NAMESPACE --ignore-not-found
kubectl create secret docker-registry gitlab-registry \
  --docker-server=$REGISTRY \
  --docker-username=$GITLAB_USER \
  --docker-password=$GITLAB_TOKEN \
  -n $NAMESPACE

# ============================================
# 3. Redis Secret 생성
# ============================================
echo "[3/12] Redis Secret 생성..."
kubectl delete secret redis-secret -n $NAMESPACE --ignore-not-found
kubectl create secret generic redis-secret \
  --from-literal=redis-password=$REDIS_PASSWORD \
  -n $NAMESPACE
kubectl label secret redis-secret -n $NAMESPACE app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate secret redis-secret -n $NAMESPACE meta.helm.sh/release-name=hiteen-infra --overwrite
kubectl annotate secret redis-secret -n $NAMESPACE meta.helm.sh/release-namespace=$NAMESPACE --overwrite

# ============================================
# 4. Firebase Secret 생성
# ============================================
echo "[4/12] Firebase Secret 생성..."
FIREBASE_KEY_FILE="hi-teen-6fa22-firebase-adminsdk-pw83b-f9b51c779f.json"
if [ -f "$FIREBASE_KEY_FILE" ]; then
  kubectl delete secret firebase-secret -n $NAMESPACE --ignore-not-found
  kubectl create secret generic firebase-secret \
    --from-file=firebase-key.json=$FIREBASE_KEY_FILE \
    -n $NAMESPACE
  echo "  ✅ Firebase Secret 생성 완료"
else
  echo "  ⚠️ $FIREBASE_KEY_FILE 파일이 없습니다."
fi

# ============================================
# 5. App Secret 생성 (DB, Mongo, JWT)
# ============================================
echo "[5/12] App Secret 생성..."
kubectl delete secret hiteen-app-secret -n $NAMESPACE --ignore-not-found
kubectl create secret generic hiteen-app-secret \
  --from-literal=db-host=10.8.3.89 \
  --from-literal=db-name=hiteen \
  --from-literal=db-user=hiteen \
  --from-literal=db-password='hiteen@2025' \
  --from-literal=mongo-host=10.8.2.156 \
  --from-literal=mongo-user=hiteen \
  --from-literal=mongo-password='hiteen@2025' \
  --from-literal=mongo-db=hiteen \
  --from-literal=jwt-secret=ac0da6c32199d5d4829ca62b05f2a353ab926e2855de718e28286ca64bc2f9df \
  -n $NAMESPACE
kubectl label secret hiteen-app-secret -n $NAMESPACE app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate secret hiteen-app-secret -n $NAMESPACE meta.helm.sh/release-name=hiteen-app --overwrite
kubectl annotate secret hiteen-app-secret -n $NAMESPACE meta.helm.sh/release-namespace=$NAMESPACE --overwrite
echo "  ✅ App Secret 생성 완료 (개발 서버 DB 사용)"

# ============================================
# 6. local-path-provisioner 확인
# ============================================
echo "[6/12] local-path-provisioner 확인..."
kubectl get storageclass local-path 2>/dev/null || {
  echo "  local-path StorageClass 설치 중..."
  kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
  sleep 5
}

# ============================================
# 7. Ingress Controller hostNetwork 설정
# ============================================
echo "[7/12] Ingress Controller hostNetwork 설정 (80/443 직접 사용)..."
kubectl get deployment ingress-nginx-controller -n ingress-nginx 2>/dev/null || {
  echo "  ⚠️ Ingress Controller가 설치되어 있지 않습니다."
  echo "  먼저 Ingress Controller를 설치해주세요:"
  echo "  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.0/deploy/static/provider/baremetal/deploy.yaml"
  exit 1
}

# hostNetwork 패치 (이미 설정되어 있으면 무시)
kubectl patch deployment ingress-nginx-controller -n ingress-nginx --type='json' -p='[
  {"op": "add", "path": "/spec/template/spec/hostNetwork", "value": true},
  {"op": "add", "path": "/spec/template/spec/dnsPolicy", "value": "ClusterFirstWithHostNet"}
]' 2>/dev/null || echo "  (이미 hostNetwork 설정됨)"

kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=120s
echo "  ✅ Ingress Controller hostNetwork 설정 완료"

# ============================================
# 8. cert-manager 설치
# ============================================
echo "[8/12] cert-manager 설치..."
if kubectl get namespace cert-manager 2>/dev/null; then
  echo "  cert-manager가 이미 설치되어 있습니다."
else
  kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml
  echo "  ⏳ cert-manager 설치 대기 중..."
  kubectl wait --for=condition=Available deployment --all -n cert-manager --timeout=120s
fi

# cert-manager hostNetwork 설정 (Pod에서 외부 HTTPS 연결 문제 해결)
kubectl patch deployment cert-manager -n cert-manager --type='json' -p='[
  {"op": "add", "path": "/spec/template/spec/hostNetwork", "value": true},
  {"op": "add", "path": "/spec/template/spec/dnsPolicy", "value": "ClusterFirstWithHostNet"}
]' 2>/dev/null || echo "  (이미 hostNetwork 설정됨)"

kubectl rollout status deployment/cert-manager -n cert-manager --timeout=120s
echo "  ✅ cert-manager 설치 완료"

# ============================================
# 9. Let's Encrypt ClusterIssuer 생성
# ============================================
echo "[9/12] Let's Encrypt ClusterIssuer 생성..."
kubectl delete secret letsencrypt-prod-key -n cert-manager --ignore-not-found

cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: $EMAIL
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - http01:
          ingress:
            class: nginx
EOF

# ClusterIssuer Ready 대기
echo "  ⏳ ClusterIssuer Ready 대기 중..."
for i in {1..30}; do
  READY=$(kubectl get clusterissuer letsencrypt-prod -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)
  if [ "$READY" == "True" ]; then
    echo "  ✅ ClusterIssuer Ready"
    break
  fi
  sleep 2
done

# ============================================
# 10. Infra Chart 배포 (Redis Cluster)
# ============================================
echo "[10/12] Infra Chart 배포 (Redis Cluster - Beta 설정)..."
INFRA_CHART_PATH="./hiteen-infra-chart"
if [ ! -d "$INFRA_CHART_PATH" ]; then
  INFRA_CHART_PATH="/root/hiteen-infra-chart"
fi

helm upgrade --install hiteen-infra $INFRA_CHART_PATH \
  -n $NAMESPACE \
  -f $INFRA_CHART_PATH/values-beta.yaml \
  --set redis.enabled=true \
  --set redis.password=$REDIS_PASSWORD \
  --set redis.storageClass=local-path \
  --wait --timeout 3m

echo "  ⏳ Redis 클러스터 준비 대기 중 (30초)..."
sleep 30

kubectl get pods -n $NAMESPACE -l app=redis

# ============================================
# 11. App Chart 배포 (Spring Boot API)
# ============================================
echo "[11/12] App Chart 배포 (Spring Boot API - Beta 설정)..."
APP_CHART_PATH="./hiteen-app-chart"
if [ ! -d "$APP_CHART_PATH" ]; then
  APP_CHART_PATH="/root/hiteen-app-chart"
fi

helm upgrade --install hiteen-app $APP_CHART_PATH \
  -n $NAMESPACE \
  -f $APP_CHART_PATH/values-beta.yaml \
  --set app.image.repository=$IMAGE \
  --set app.image.tag=$TAG \
  --set nfs.enabled=true \
  --set nfs.server=$NFS_SERVER \
  --set nfs.path=$NFS_PATH \
  --set nfs.storage=100Gi \
  --set metrics.enabled=false \
  --set metrics.serviceMonitor.enabled=false \
  --wait --timeout 4m

# ============================================
# 12. SSL 인증서 발급 확인
# ============================================
echo "[12/12] SSL 인증서 발급 확인..."
echo "  ⏳ 인증서 발급 대기 중 (최대 2분)..."

for i in {1..60}; do
  READY=$(kubectl get certificate hiteen-beta-tls -n $NAMESPACE -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)
  if [ "$READY" == "True" ]; then
    echo "  ✅ SSL 인증서 발급 완료!"
    break
  fi
  if [ $i -eq 60 ]; then
    echo "  ⚠️ 인증서 발급이 아직 진행 중입니다. 나중에 확인하세요:"
    echo "     kubectl get certificate -n $NAMESPACE"
  fi
  sleep 2
done

echo ""
echo "=========================================="
echo "✅ 개발(Beta) 서버 배포 완료!"
echo "=========================================="

# 상태 확인
echo ""
echo "📋 Pod 상태:"
kubectl get pods -n $NAMESPACE -o wide

echo ""
echo "📋 Service 상태:"
kubectl get svc -n $NAMESPACE

echo ""
echo "📋 PV/PVC 상태:"
kubectl get pvc -n $NAMESPACE

echo ""
echo "📋 Ingress 상태:"
kubectl get ingress -n $NAMESPACE

echo ""
echo "📋 SSL 인증서 상태:"
kubectl get certificate -n $NAMESPACE

echo ""
echo "=========================================="
echo "🔗 테스트 URL: https://$DOMAIN/actuator/health"
echo "=========================================="

