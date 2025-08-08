#!/bin/bash
#
# HITEEN Kubernetes Resource Installer
#
# Licensed under the Apache License, Version 2.0 (the "License");
#
# chmod +x hiteen-install.sh
# ./hiteen-install.sh
#
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

echo "=========================================="
echo "HITEEN 서비스용 PostgreSQL & MongoDB 배포 완료!"
echo "각각의 서비스 접속정보는 아래와 같습니다:"
echo ""
echo "PostgreSQL: hiteen-postgres.hiteen.svc.cluster.local:55432"
echo "MongoDB   : hiteen-mongo.hiteen.svc.cluster.local:27017"
echo "네임스페이스: hiteen"
echo "=========================================="
