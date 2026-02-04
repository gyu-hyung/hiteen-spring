# 운영 서버 배포 가이드

## 📋 개요

운영 서버 배포를 위한 완전한 가이드입니다.
인프라와 애플리케이션 차트가 분리되어 관리됩니다.

## 📁 차트 구조

```
├── hiteen-infra-chart/     # 인프라 (Redis, NFS, DB Backup)
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│       ├── redis-cluster.yaml
│       ├── nfs-assets.yaml
│       ├── nfs-backup.yaml
│       └── db-backup-cronjob.yaml
│
├── hiteen-app-chart/       # 애플리케이션 (Spring Boot API)
│   ├── Chart.yaml
│   ├── values.yaml         # Production
│   ├── values-dev.yaml     # Development
│   └── templates/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── ingress.yaml
│       ├── hpa.yaml
│       ├── pdb.yaml
│       └── servicemonitor.yaml
│
└── .gitlab-ci.yml          # GitLab CI/CD Pipeline
```

## 🚀 배포 순서

### 1. 네임스페이스 및 시크릿 생성

```bash
# 운영 네임스페이스 생성
kubectl create ns hiteen-prod

# GitLab Registry Secret 생성
kubectl create secret docker-registry gitlab-registry-secret \
  --docker-server=registry.gitlab.com \
  --docker-username=<GITLAB_USERNAME> \
  --docker-password=<GITLAB_TOKEN> \
  -n hiteen-prod

# Firebase Secret 생성
kubectl create secret generic firebase-secret \
  --from-file=firebase-key.json=/path/to/firebase-key.json \
  -n hiteen-prod
```

### 2. local-path-provisioner 설치 (Redis PVC용)

```bash
kubectl create ns local-path-storage
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml

# 확인
kubectl get pods -n local-path-storage
kubectl get storageclass
```

### 3. Ingress Controller 설치

```bash
kubectl create namespace ingress-nginx
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.3/deploy/static/provider/baremetal/deploy.yaml

# NodePort 확인
kubectl get svc -n ingress-nginx
```

### 4. 인프라 차트 배포

```bash
# values.yaml의 실제 값들을 설정 후 배포
helm upgrade --install hiteen-infra ./hiteen-infra-chart \
  -n hiteen-prod \
  --set redis.password=<REDIS_PASSWORD> \
  --set dbBackup.postgres.password=<DB_PASSWORD> \
  --set nfs.server=<NFS_SERVER_IP> \
  --set backupNfs.server=<NFS_SERVER_IP> \
  --set dbBackup.postgres.host=<DB_HOST>

# Redis 클러스터 상태 확인
kubectl exec -n hiteen-prod redis-0 -- redis-cli -a <REDIS_PASSWORD> cluster info
kubectl exec -n hiteen-prod redis-0 -- redis-cli -a <REDIS_PASSWORD> cluster nodes
```


📋 GitLab에서 설정해야 할 CI/CD Variables
GitLab 프로젝트 → Settings → CI/CD → Variables에서 추가:
Variable  설명  예시

KUBE_CONFIG_DEV     개발 K8s kubeconfig (base64)       cat ~/.kube/config | base64 -w 0
KUBE_CONFIG_PROD    운영 K8s kubeconfig (base64)       cat ~/.kube/config | base64 -w 0
CI_DEPLOY_USER      GitLab 사용자명                     gud5603@gmail.com
CI_DEPLOY_PASSWORD  GitLab 비밀번호 or Access Token     glpat-xxx
FIREBASE_KEY_JSON   Firebase 키 (base64)              cat firebase-key.json | base64
REDIS_PASSWORD      개발 Redis 비밀번호                  xxxxxxxx
REDIS_PASSWORD_PROD 운영 Redis 비밀번호                  새로운_강력한_비밀번호




### 5. 애플리케이션 차트 배포

```bash
# 시크릿과 함께 배포
helm upgrade --install hiteen-app ./hiteen-app-chart \
  -n hiteen-prod \
  --set app.image.repository=registry.gitlab.com/your-group/hiteen-api \
  --set app.image.tag=prod-<COMMIT_SHA> \
  --set secrets.db.password=<DB_PASSWORD> \
  --set secrets.mongo.password=<MONGO_PASSWORD> \
  --set secrets.jwt.secret=<JWT_SECRET> \
  -f ./hiteen-app-chart/values.yaml

# 배포 상태 확인
kubectl get pods -n hiteen-prod
kubectl get svc -n hiteen-prod
kubectl get ingress -n hiteen-prod
```

### 6. 시크릿 파일로 관리 (권장)

민감한 정보는 별도의 `secrets.yaml` 파일로 관리하고 Git에서 제외하세요:

```bash
# secrets-prod.yaml (gitignore에 추가)
cat > secrets-prod.yaml << 'EOF'
secrets:
  db:
    host: "10.8.0.200"
    name: "hiteen-prod"
    user: "hiteen"
    password: "your-db-password"
  mongo:
    host: "10.8.0.200"
    user: "hiteen"
    password: "your-mongo-password"
    db: "hiteen"
  jwt:
    secret: "your-jwt-secret-key"
  sms:
    apiKey: "your-sms-api-key"
    kakaoSenderKey: "your-kakao-sender-key"
  giftishow:
    authKey: "your-giftishow-auth-key"
    tokenKey: "your-giftishow-token-key"
  external:
    neisApiKey: "your-neis-api-key"
    kakaoApiKey: "your-kakao-api-key"
EOF

# 시크릿 파일과 함께 배포
helm upgrade --install hiteen-app ./hiteen-app-chart \
  -n hiteen-prod \
  -f ./hiteen-app-chart/values.yaml \
  -f ./secrets-prod.yaml \
  --set app.image.tag=prod-<COMMIT_SHA>
```

## 🗄️ 백업 필요 테이블

운영 서버 초기 데이터 마이그레이션 시 아래 테이블들을 백업/복원해야 합니다:

```sql
-- 필수 기본 데이터
select * from api_keys;
select * from assets;
select * from cash_rules;
select * from challenge_reward_policy;
select * from codes;              -- asset 참조
select * from exp_actions;
select * from games;
select * from goods_brand;
select * from goods_category;
select * from goods_giftishow;    -- asset 참조
select * from interests;
select * from point_rules;
select * from poll_templates;
select * from question;           -- asset 참조
select * from question_2;         -- asset 참조
select * from school_classes;
select * from school_food;
select * from schools;
select * from terms;
select * from tiers;
select * from time_table;
```

### 데이터 마이그레이션 스크립트

```bash
# 개발 DB에서 덤프
pg_dump -h <DEV_DB_HOST> -U hiteen -d hiteen2-dev \
  -t api_keys -t assets -t cash_rules -t challenge_reward_policy \
  -t codes -t exp_actions -t games -t goods_brand -t goods_category \
  -t goods_giftishow -t interests -t point_rules -t poll_templates \
  -t question -t question_2 -t school_classes -t school_food \
  -t schools -t terms -t tiers -t time_table \
  -F c -f hiteen-init-data.dump

# 운영 DB로 복원
pg_restore -h <PROD_DB_HOST> -U hiteen -d hiteen-prod \
  --clean --if-exists \
  hiteen-init-data.dump
```

## 🔧 GitLab CI/CD 설정

### 필수 CI/CD 변수 (GitLab > Settings > CI/CD > Variables)

| Variable | Description |
|----------|-------------|
| `KUBE_CONFIG_DEV` | 개발 클러스터 kubeconfig (base64) |
| `KUBE_CONFIG_PROD` | 운영 클러스터 kubeconfig (base64) |

```bash
# kubeconfig base64 인코딩
cat ~/.kube/config | base64 -w 0
```

## 📊 모니터링

### Prometheus + Grafana 설정

```bash
# kube-prometheus-stack 설치
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
```

### ServiceMonitor가 활성화되면 자동으로 메트릭 수집됨

- Endpoint: `/actuator/prometheus`
- Interval: 15s

## ⚠️ Redis Cluster 주의사항

1. **클러스터 초기화**: 첫 배포 시 Helm hook으로 자동 초기화됨
2. **수동 초기화 필요시**:
```bash
kubectl exec -it -n hiteen-prod redis-0 -- redis-cli -a <PASSWORD> --cluster create \
  redis-0.redis.hiteen-prod.svc.cluster.local:6379 \
  redis-1.redis.hiteen-prod.svc.cluster.local:6379 \
  redis-2.redis.hiteen-prod.svc.cluster.local:6379 \
  redis-3.redis.hiteen-prod.svc.cluster.local:6379 \
  redis-4.redis.hiteen-prod.svc.cluster.local:6379 \
  redis-5.redis.hiteen-prod.svc.cluster.local:6379 \
  --cluster-replicas 1
```

3. **클러스터 리셋**:
```bash
kubectl delete statefulset redis -n hiteen-prod
kubectl delete pvc -n hiteen-prod -l app=redis
```

## 🔄 롤백

```bash
# 이전 버전으로 롤백
helm rollback hiteen-app <REVISION> -n hiteen-prod

# 히스토리 확인
helm history hiteen-app -n hiteen-prod
```

## 📝 체크리스트

- [ ] 네임스페이스 생성
- [ ] GitLab Registry Secret 생성
- [ ] Firebase Secret 생성
- [ ] local-path-provisioner 설치
- [ ] Ingress Controller 설치
- [ ] NFS 서버 설정 및 마운트 테스트
- [ ] 인프라 차트 배포 (Redis, NFS PV/PVC, Backup CronJob)
- [ ] Redis 클러스터 초기화 확인
- [ ] 초기 데이터 마이그레이션
- [ ] 애플리케이션 차트 배포
- [ ] Ingress/도메인 연결 확인
- [ ] TLS 인증서 발급 확인
- [ ] 모니터링 설정 확인
- [ ] 백업 CronJob 동작 확인


