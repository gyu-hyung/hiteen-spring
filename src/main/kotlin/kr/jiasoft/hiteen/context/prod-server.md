# 운영 서버 배포 계획

## ✅ 완료된 작업

### 1. Helm Chart 분리 완료
- **hiteen-infra-chart**: 인프라 (Redis Cluster, NFS PV/PVC, DB Backup CronJob)
- **hiteen-app-chart**: 애플리케이션 (Deployment, Service, Ingress, HPA, PDB, Secrets)

### 2. GitLab CI/CD 파일 생성
- `.gitlab-ci.yml` 생성 완료
- develop 브랜치 → 개발서버 자동 배포
- main 브랜치 → 운영서버 수동 배포 (승인 필요)

### 3. Spring Profile 추가
- `application-prod.yml` 생성 완료
- 환경변수를 통한 민감 정보 주입

### 4. 상세 배포 가이드
- `/docs/PRODUCTION-DEPLOY.md` - 운영 배포 가이드
- `/docs/K8S-CLUSTER-SETUP.md` - K8s 클러스터 구축 가이드 (Rocky/Alma)
- `/docs/K8S-CLUSTER-SETUP-UBUNTU.md` - K8s 클러스터 구축 가이드 (Ubuntu)
- `/docs/PRODUCTION-CHECKLIST.md` - 운영 배포 체크리스트

### 5. 자동화 스크립트
- `/scripts/k8s-node-init.sh` - 노드 공통 초기화
- `/scripts/k8s-master-setup.sh` - 마스터 노드 설정
- `/scripts/k8s-components-install.sh` - 필수 컴포넌트 설치
- `/scripts/nfs-server-setup.sh` - NFS 서버 설정

## 📁 생성된 파일 구조

```
├── hiteen-infra-chart/
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│       ├── redis-cluster.yaml
│       ├── nfs-assets.yaml
│       ├── nfs-backup.yaml
│       └── db-backup-cronjob.yaml
│
├── hiteen-app-chart/
│   ├── Chart.yaml
│   ├── values.yaml         # Production
│   ├── values-dev.yaml     # Development
│   └── templates/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── ingress.yaml
│       ├── hpa.yaml
│       ├── pdb.yaml
│       ├── secrets.yaml
│       └── servicemonitor.yaml
│
├── .gitlab-ci.yml
└── src/main/resources/
    └── application-prod.yml
```

## 📋 배포 체크리스트

```bash
# 1. 네임스페이스 생성
kubectl create ns hiteen-prod

# 2. Secret 생성
kubectl create secret docker-registry gitlab-registry-secret \
  --docker-server=registry.gitlab.com \
  --docker-username=<USER> \
  --docker-password=<TOKEN> \
  -n hiteen-prod

kubectl create secret generic firebase-secret \
  --from-file=firebase-key.json=/path/to/key.json \
  -n hiteen-prod

# 3. 인프라 배포
helm upgrade --install hiteen-infra ./hiteen-infra-chart \
  -n hiteen-prod \
  --set redis.password=<REDIS_PW> \
  --set dbBackup.postgres.password=<DB_PW> \
  --set nfs.server=<NFS_IP> \
  --set backupNfs.server=<NFS_IP>

# 4. 앱 배포 (시크릿 파일 사용)
helm upgrade --install hiteen-app ./hiteen-app-chart \
  -n hiteen-prod \
  -f ./hiteen-app-chart/values.yaml \
  -f ./secrets-prod.yaml \
  --set app.image.tag=<TAG>
```

## 🗄️ 백업 필요한 테이블들 

```sql
select * from api_keys;
select * from assets;
select * from cash_rules;
select * from challenge_reward_policy crp ;
select * from codes;--asset
select * from exp_actions;
select * from games;
select * from goods_brand;
select * from goods_category;
select * from goods_giftishow;--asset

select * from interests;
select * from point_rules;
select * from poll_templates;
select * from question;--asset
select * from question_2;--asset
select * from school_classes;
select * from school_food;
select * from schools;
select * from terms;
select * from tiers;

select * from time_table;
```

## 🔴 Redis Cluster 결정 사항

Redis Cluster는 다음과 같이 구성:
- **Replicas**: 6개 (Master 3, Slave 3)
- **clusterReplicas**: 1 (각 마스터당 1개의 슬레이브)
- **자동 초기화**: Helm post-install hook으로 자동 클러스터 생성
- **수동 초기화 필요시**: `/docs/PRODUCTION-DEPLOY.md` 참조
