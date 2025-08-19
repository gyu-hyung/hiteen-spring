#
#
# chmod +x hiteen-delete.sh
# ./hiteen-delete.sh
#
#

#!/bin/bash
set -e

# PostgreSQL
kubectl delete deployment hiteen-postgres -n hiteen || true
kubectl delete svc hiteen-postgres -n hiteen || true
kubectl delete pvc hiteen-postgres-pvc -n hiteen || true

# MongoDB
kubectl delete deployment hiteen-mongo -n hiteen || true
kubectl delete svc hiteen-mongo -n hiteen || true
kubectl delete pvc hiteen-mongo-pvc -n hiteen || true

# Redis
kubectl delete deployment hiteen-redis -n hiteen || true
kubectl delete svc hiteen-redis -n hiteen || true
kubectl delete pvc hiteen-redis-pvc -n hiteen || true

echo "HITEEN PostgreSQL, MongoDB & Redis 리소스 삭제 완료!"
