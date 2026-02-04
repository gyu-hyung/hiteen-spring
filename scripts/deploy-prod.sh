#!/bin/bash
# ============================================
# 운영 서버 수동 배포 스크립트
# 마스터 노드에서 실행
# hiteen-infra-chart -> hiteen-app-chart 순서로 배포
# ============================================

set -e

# 설정
NAMESPACE="hiteen"
REGISTRY="gitlab.barunsoft.net:6005"
IMAGE="gitlab.barunsoft.net:6005/jiasoft/hiteen2-server"
TAG="${1:-prod-0.0.1}"

# GitLab Registry 인증 정보
GITLAB_USER="gud5603@gmail.com"
GITLAB_TOKEN="glpat-xxxxxxxxxxxxxxxxxxxx"

# Redis 비밀번호
REDIS_PASSWORD="xxxxxxxx"

# NFS 설정
NFS_SERVER="10.8.0.159"
NFS_PATH="/srv/nfs/assets"

echo "=========================================="
echo "🚀 Hiteen 운영 서버 배포 시작"
echo "=========================================="
echo "Namespace: $NAMESPACE"
echo "Image: $IMAGE:$TAG"
echo "NFS Server: $NFS_SERVER"
echo ""

# 1. Namespace 생성
echo "[1/8] Namespace 생성..."
kubectl get ns $NAMESPACE 2>/dev/null || kubectl create ns $NAMESPACE

# 2. GitLab Registry Secret 생성
echo "[2/8] GitLab Registry Secret 생성..."
kubectl delete secret gitlab-registry -n $NAMESPACE --ignore-not-found
kubectl create secret docker-registry gitlab-registry \
  --docker-server=$REGISTRY \
  --docker-username=$GITLAB_USER \
  --docker-password=$GITLAB_TOKEN \
  -n $NAMESPACE

# 3. Redis Secret 생성
echo "[3/8] Redis Secret 생성..."
kubectl delete secret redis-secret -n $NAMESPACE --ignore-not-found
kubectl create secret generic redis-secret \
  --from-literal=redis-password=$REDIS_PASSWORD \
  -n $NAMESPACE
kubectl label secret redis-secret -n $NAMESPACE app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate secret redis-secret -n $NAMESPACE meta.helm.sh/release-name=hiteen-infra --overwrite
kubectl annotate secret redis-secret -n $NAMESPACE meta.helm.sh/release-namespace=$NAMESPACE --overwrite

# 4. Firebase Secret 생성
echo "[4/8] Firebase Secret 생성..."
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

# 5. App Secret 생성 (DB, Mongo, JWT)
echo "[5/8] App Secret 생성..."
kubectl delete secret hiteen-app-secret -n $NAMESPACE --ignore-not-found
kubectl create secret generic hiteen-app-secret \
  --from-literal=db-host=49.247.175.76 \
  --from-literal=db-name=hiteen \
  --from-literal=db-user=hiteen \
  --from-literal=db-password='xxxxxxxx' \
  --from-literal=mongo-host=49.247.170.182 \
  --from-literal=mongo-user=hiteen \
  --from-literal=mongo-password='xxxxxxxx' \
  --from-literal=mongo-db=hiteen \
  --from-literal=jwt-secret=ac0da6c32199d5d4829ca62b05f2a353ab926e2855de718e28286ca64bc2f9df \
  -n $NAMESPACE
kubectl label secret hiteen-app-secret -n $NAMESPACE app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate secret hiteen-app-secret -n $NAMESPACE meta.helm.sh/release-name=hiteen-app --overwrite
kubectl annotate secret hiteen-app-secret -n $NAMESPACE meta.helm.sh/release-namespace=$NAMESPACE --overwrite
echo "  ✅ App Secret 생성 완료 (개발 서버 DB 사용)"

# 6. local-path-provisioner 확인
echo "[6/8] local-path-provisioner 확인..."
kubectl get storageclass local-path || {
  echo "  local-path StorageClass 설치 중..."
  kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
  sleep 5
}

# 7. Infra Chart 배포 (Redis Cluster)
echo "[7/8] Infra Chart 배포 (Redis Cluster)..."
INFRA_CHART_PATH="./hiteen-infra-chart"
if [ ! -d "$INFRA_CHART_PATH" ]; then
  INFRA_CHART_PATH="/root/hiteen-infra-chart"
fi

helm upgrade --install hiteen-infra $INFRA_CHART_PATH \
  -n $NAMESPACE \
  --set redis.enabled=true \
  --set redis.password=$REDIS_PASSWORD \
  --set redis.storageClass=local-path \
  --wait --timeout 2m

echo "  ⏳ Redis 클러스터 준비 대기 중 (30초)..."
sleep 30

kubectl get pods -n $NAMESPACE -l app=redis

# 8. App Chart 배포 (Spring Boot API)
echo "[8/8] App Chart 배포 (Spring Boot API)..."
APP_CHART_PATH="./hiteen-app-chart"
if [ ! -d "$APP_CHART_PATH" ]; then
  APP_CHART_PATH="/root/hiteen-app-chart"
fi

helm upgrade --install hiteen-app $APP_CHART_PATH \
  -n $NAMESPACE \
  --set app.image.repository=$IMAGE \
  --set app.image.tag=$TAG \
  --set nfs.enabled=true \
  --set nfs.server=$NFS_SERVER \
  --set nfs.path=$NFS_PATH \
  --set nfs.storage=100Gi \
  --wait --timeout 4m

echo ""
echo "=========================================="
echo "✅ 배포 완료!"
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
kubectl get pv
kubectl get pvc -n $NAMESPACE

echo ""
echo "📋 Ingress 상태:"
kubectl get ingress -n $NAMESPACE

