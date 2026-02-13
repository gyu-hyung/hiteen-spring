# 📚 Hiteen 운영 문서

## 문서 목록

| 문서 | 설명 |
|------|------|
| [K8S-CLUSTER-SETUP.md](./K8S-CLUSTER-SETUP.md) | Kubernetes 클러스터 구축 가이드 (Rocky Linux/AlmaLinux) |
| [K8S-CLUSTER-SETUP-UBUNTU.md](./K8S-CLUSTER-SETUP-UBUNTU.md) | Kubernetes 클러스터 구축 가이드 (Ubuntu 22.04) |
| [PRODUCTION-DEPLOY.md](./PRODUCTION-DEPLOY.md) | 운영 환경 배포 가이드 |
| [PRODUCTION-CHECKLIST.md](./PRODUCTION-CHECKLIST.md) | 운영 배포 체크리스트 |

## 스크립트

| 스크립트 | 설명 |
|----------|------|
| [k8s-node-init.sh](../scripts/k8s-node-init.sh) | 모든 노드 공통 초기화 |
| [k8s-master-setup.sh](../scripts/k8s-master-setup.sh) | 마스터 노드 설정 |
| [k8s-components-install.sh](../scripts/k8s-components-install.sh) | 필수 컴포넌트 설치 |
| [nfs-server-setup.sh](../scripts/nfs-server-setup.sh) | NFS 서버 설정 |

## Helm Charts

| Chart | 설명 |
|-------|------|
| [hiteen-infra-chart](../hiteen-infra-chart/) | 인프라 (Redis, NFS, Backup) |
| [hiteen-app-chart](../hiteen-app-chart/) | 애플리케이션 (Backend API) |

## 빠른 시작

### 1. 클러스터 구축

```bash
# 1. 모든 노드에서 초기화 스크립트 실행
curl -sO https://raw.githubusercontent.com/.../scripts/k8s-node-init.sh
chmod +x k8s-node-init.sh
./k8s-node-init.sh

# 2. 마스터 노드에서 설정 스크립트 실행
./k8s-master-setup.sh

# 3. 워커 노드에서 join 명령어 실행
kubeadm join <master-ip>:6443 --token ... --discovery-token-ca-cert-hash ...

# 4. 마스터에서 컴포넌트 설치
./k8s-components-install.sh
```

### 2. 애플리케이션 배포

```bash
# 1. 네임스페이스 및 시크릿 생성
kubectl create ns hiteen
kubectl create secret ...

# 2. 인프라 배포
helm upgrade --install hiteen-infra ./hiteen-infra-chart -n hiteen ...

# 3. 앱 배포
helm upgrade --install hiteen-app ./hiteen-app-chart -n hiteen ...
```

자세한 내용은 각 문서를 참조하세요.

### 3. DB 백업 및 복원

```bash
# 백업
PGPASSWORD='hiteen@2025' pg_dump -h 49.247.175.76 -p 5432 -U hiteen -d hiteen -F p -f /Users/jogyuhyeong/Documents/dev/hiteen2-server/hiteen_20260213.dump


PGPASSWORD='hiteen@2025' psql -h 49.247.175.76 -p 5432 -U hiteen -d hiteen -f hiteen_20260213.dump
