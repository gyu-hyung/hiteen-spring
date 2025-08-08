#
#
# chmod +x hiteen-delete.sh
# ./hiteen-delete.sh
#
#



#!/bin/bash
set -e

kubectl delete deployment hiteen-postgres -n hiteen || true
kubectl delete svc hiteen-postgres -n hiteen || true
kubectl delete pvc hiteen-postgres-pvc -n hiteen || true

kubectl delete deployment hiteen-mongo -n hiteen || true
kubectl delete svc hiteen-mongo -n hiteen || true
kubectl delete pvc hiteen-mongo-pvc -n hiteen || true

echo "HITEEN PostgreSQL & MongoDB 리소스 삭제 완료!"