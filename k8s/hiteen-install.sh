#!/bin/bash
#
# HITEEN Kubernetes Resource Installer
#
# chmod +x hiteen-install.sh
# ./hiteen-install.sh
#

set -e

# 1. 네임스페이스 생성 (이미 있으면 무시)
kubectl apply -f hiteen-namespace.yml

# 2. context 네임스페이스 지정 (kubectl 명령 디폴트 ns를 hiteen으로)
kubectl config set-context $(kubectl config current-context) --namespace=hiteen

# 3. PostgreSQL(5432) 배포
kubectl apply -f hiteen-postgres.yml

# 4. PostgreSQL Pod 올라올 때까지 대기 (Deployment 기준)
kubectl rollout status deployment/hiteen-postgres

# 5. MongoDB 배포
kubectl apply -f hiteen-mongo.yml

# 6. MongoDB Pod 올라올 때까지 대기 (Deployment 기준)
kubectl rollout status deployment/hiteen-mongo

# 7. Redis 배포
kubectl apply -f hiteen-redis.yml

# 8. Redis Pod 올라올 때까지 대기 (Deployment 기준)
kubectl rollout status deployment/hiteen-redis

# 9. Soketi 배포
kubectl apply -f hiteen-soketi.yml

# 10. Soketi Pod 올라올 때까지 대기 (Deployment 기준)
kubectl rollout status deployment/hiteen-soketi

# (선택) NodePort 정보 출력용 변수
REDIS_NODEPORT=$(kubectl get svc hiteen-redis -o jsonpath='{.spec.ports[0].nodePort}' || echo "N/A")
MINIKUBE_IP=$(minikube ip 2>/dev/null || echo "N/A")
echo "=========================================="
echo "HITEEN 서비스용 PostgreSQL, MongoDB, Redis 배포 완료!"
echo ""
echo "네임스페이스: hiteen"
echo ""
echo "PostgreSQL: hiteen-postgres.hiteen.svc.cluster.local:5432"
echo "MongoDB   : hiteen-mongo.hiteen.svc.cluster.local:27017"
echo "Redis     : hiteen-redis.hiteen.svc.cluster.local:6379"
echo "Soketi    : hiteen-soketi.hiteen.svc.cluster.local:6001 (ws), 9601 (metrics)"
echo ""
echo "로컬 포워딩 예시:"
echo ""
echo "  PostgreSQL → localhost:5432"
echo "    kubectl port-forward svc/hiteen-postgres -n hiteen 5432:5432"
echo ""
echo "  MongoDB → localhost:27017"
echo "    kubectl port-forward svc/hiteen-mongo -n hiteen 27017:27017"
echo ""
echo "  Redis → localhost:6379"
echo "    kubectl port-forward svc/hiteen-redis -n hiteen 6379:6379"
echo ""
echo "  Soketi → localhost:6001 (ws), localhost:9601 (metrics)"
echo "    kubectl port-forward svc/hiteen-soketi -n hiteen 6001:6001 9601:9601"
echo ""
echo "Minikube NodePort 접속:"
echo "  IP       : \${MINIKUBE_IP}"
echo "  NodePort : \${REDIS_NODEPORT}"
echo "  redis-cli -h \${MINIKUBE_IP} -p \${REDIS_NODEPORT} -a xxxxxxxx ping"
echo "=========================================="