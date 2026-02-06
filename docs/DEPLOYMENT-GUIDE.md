# 운영 서버 배포 상세 가이드

## 📋 전체 배포 흐름

```
[GitLab] 코드 푸시 → CI/CD 빌드 → GitLab Registry 푸시 → K8s 배포
                                ↓
[K8s 클러스터] ← Docker 이미지 Pull ← GitLab Registry
                    ↓
              Pod 생성 및 실행
```

---

## 1️⃣ Container Registry 구성

**GitLab Container Registry**를 사용합니다.

| 역할 | 서비스 |
|------|--------|
| 소스 코드 관리 | GitLab (gitlab.barunsoft.net) |
| CI/CD 파이프라인 | GitLab CI/CD |
| 컨테이너 이미지 저장 | GitLab Registry (gitlab.barunsoft.net:6005) |

### 이미지 주소
```
gitlab.barunsoft.net:6005/<group>/<project>:<태그>

# 예시
gitlab.barunsoft.net:6005/jiasoft/hiteen2-server:prod-abc1234
gitlab.barunsoft.net:6005/jiasoft/hiteen2-server:0.0.1
gitlab.barunsoft.net:6005/jiasoft/hiteen2-server:latest
```

---

## 2️⃣ 배포 준비 (최초 1회)

### Step 1: GitLab Deploy Token 생성 (K8s용)

1. GitLab 프로젝트 → **Settings** → **Repository** → **Deploy tokens**
2. 새 토큰 생성:
   - **Name**: k8s-deploy
   - **Scopes**: `read_registry` 체크
3. **Username**과 **Token** 복사

### Step 2: K8s에 Registry Secret 생성

```bash
# 마스터 노드에서 실행

# 개발 클러스터
kubectl create secret docker-registry gitlab-registry \
  --docker-server=gitlab.barunsoft.net:6005 \
  --docker-username=<DEPLOY_TOKEN_USERNAME> \
  --docker-password=<DEPLOY_TOKEN> \
  -n hiteen

# 운영 클러스터
kubectl create secret docker-registry gitlab-registry \
  --docker-server=gitlab.barunsoft.net:6005 \
  --docker-username=<DEPLOY_TOKEN_USERNAME> \
  --docker-password=<DEPLOY_TOKEN> \
  -n hiteen-prod
```

### Step 3: GitLab CI/CD Variables 설정

GitLab 프로젝트 → **Settings** → **CI/CD** → **Variables**:

| Variable | Value | Protected | Masked |
|----------|-------|-----------|--------|
| `KUBE_CONFIG_DEV` | (base64 kubeconfig) | Yes | Yes |
| `KUBE_CONFIG_PROD` | (base64 kubeconfig) | Yes | Yes |

> 📝 `CI_REGISTRY`, `CI_REGISTRY_USER`, `CI_REGISTRY_PASSWORD`는 GitLab CI에서 자동 제공됩니다.

### Step 4: 로컬에서 GitLab Registry 로그인 (수동 배포 시)

```bash
docker login gitlab.barunsoft.net:6005
# Username: <GitLab 사용자명>
# Password: <GitLab 비밀번호 또는 Personal Access Token>
```

---

## 3️⃣ Docker 이미지 빌드 및 푸시

### 방법 A: 로컬에서 수동 빌드

```bash
# 1. 프로젝트 루트에서 빌드
cd /path/to/hiteen2-server

# 2. 스크립트 사용 (권장)
./scripts/deploy-gitlab.sh

# 또는 직접 실행 (플랫폼 지정 및 푸시 동시 수행)
TAG=0.0.1
docker build --no-cache --platform linux/amd64 -t gitlab.barunsoft.net:6005/jiasoft/hiteen2-server:prod-$TAG . --push
```

### 방법 B: GitLab CI/CD 자동 빌드 (권장)

`.gitlab-ci.yml`이 설정되어 있으면:
1. `develop` 브랜치 푸시 → 개발서버 자동 배포
2. `main` 브랜치 푸시 → 빌드 후 **운영 배포는 수동 승인**

---

## 4️⃣ Helm 차트로 K8s 배포

### Step 1: 네임스페이스 생성 (최초 1회)

```bash
kubectl create ns hiteen-prod
```

### Step 2: Secrets 생성 (최초 1회)

```bash
# GitLab Registry Secret
kubectl create secret docker-registry gitlab-registry \
  --docker-server=gitlab.barunsoft.net:6005 \
  --docker-username=<DEPLOY_TOKEN_USERNAME> \
  --docker-password=<DEPLOY_TOKEN> \
  -n hiteen-prod

# Firebase Credentials
kubectl create secret generic firebase-secret \
  --from-file=firebase-key.json=/path/to/firebase-key.json \
  -n hiteen-prod

# Redis Secret
kubectl create secret generic redis-secret \
  --from-literal=redis-password=<REDIS_PASSWORD> \
  -n hiteen-prod
```

### Step 3: secrets-prod.yaml 파일 작성

```bash
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
    secret: "your-jwt-secret-key-min-32-characters"
EOF
```

### Step 4: 인프라 배포 (Redis, NFS)

```bash
helm upgrade --install hiteen-infra ./hiteen-infra-chart \
  -n hiteen-prod \
  --set redis.password=<REDIS_PASSWORD> \
  --set nfs.server=<NFS_SERVER_IP> \
  --set backupNfs.server=<NFS_SERVER_IP> \
  --set dbBackup.postgres.host=<DB_HOST> \
  --set dbBackup.postgres.password=<DB_PASSWORD>
```

### Step 5: 애플리케이션 배포

```bash
helm upgrade --install hiteen-app ./hiteen-app-chart \
  -n hiteen-prod \
  -f ./hiteen-app-chart/values.yaml \
  -f ./secrets-prod.yaml \
  --set app.image.tag=0.0.1
```

---

## 5️⃣ 배포 확인

```bash
# Pod 상태 확인
kubectl get pods -n hiteen-prod

# 로그 확인
kubectl logs -f deployment/hiteen-api -n hiteen-prod

# 서비스 확인
kubectl get svc -n hiteen-prod

# Ingress 확인
kubectl get ingress -n hiteen-prod

# 이벤트 확인 (문제 발생 시)
kubectl get events -n hiteen-prod --sort-by='.lastTimestamp'
```

---

## 6️⃣ 업데이트 배포 (Rolling Update)

### 새 버전 배포

```bash
# 1. 새 이미지 빌드 & 푸시 (로컬에서)
TAG=0.0.1

# 플랫폼 지정 및 푸시까지 한번에 수행
docker build \
  --platform linux/amd64 \
  -t gitlab.barunsoft.net:6005/jiasoft/hiteen2-server:prod-$TAG \
  . --push

# 2. Helm으로 업데이트
helm upgrade --install hiteen-app ./hiteen-app-chart \
  -n hiteen-prod \
  -f ./hiteen-app-chart/values.yaml \
  -f ./secrets-prod.yaml \
  --set app.image.tag=$TAG

# 3. 롤아웃 상태 확인
kubectl rollout status deployment/hiteen-api -n hiteen-prod
```

### 롤백 (문제 발생 시)

```bash
# Helm 히스토리 확인
helm history hiteen-app -n hiteen-prod

# 이전 버전으로 롤백
helm rollback hiteen-app <REVISION> -n hiteen-prod

# 또는 kubectl로 롤백
kubectl rollout undo deployment/hiteen-api -n hiteen-prod
```

---

## 7️⃣ 전체 배포 순서 요약

```
┌─────────────────────────────────────────────────────────────┐
│ 1. 최초 설정 (1회)                                           │
├─────────────────────────────────────────────────────────────┤
│  □ GitLab Access Token 생성                                 │
│  □ K8s에 gitlab-registry 생성                               │
│  □ K8s에 firebase-secret 생성                               │
│  □ K8s에 redis-secret 생성                                  │
│  □ secrets-prod.yaml 파일 작성                              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. 인프라 배포 (1회, 변경 시만 재배포)                        │
├─────────────────────────────────────────────────────────────┤
│  □ helm upgrade --install hiteen-infra ...                  │
│  □ Redis Cluster 상태 확인                                  │
│  □ NFS PV/PVC 상태 확인                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. 앱 배포 (매 릴리즈)                                       │
├─────────────────────────────────────────────────────────────┤
│  □ docker build & push (또는 GitLab CI 자동)                │
│  □ helm upgrade --install hiteen-app ...                    │
│  □ kubectl get pods -n hiteen-prod (Running 확인)           │
│  □ API 헬스체크 확인                                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 8️⃣ 유용한 명령어 모음

```bash
# 이미지 Pull 테스트
kubectl run test-pull --image=gitlab.barunsoft.net:6005/jiasoft/hiteen2-server:latest \
  --overrides='{"spec":{"imagePullSecrets":[{"name":"gitlab-registry"}]}}' \
  -n hiteen-prod --rm -it --restart=Never -- echo "Pull Success!"

# Pod 접속
kubectl exec -it deployment/hiteen-api -n hiteen-prod -- /bin/sh

# 실시간 로그
kubectl logs -f deployment/hiteen-api -n hiteen-prod

# 리소스 사용량
kubectl top pods -n hiteen-prod

# 시크릿 확인
kubectl get secrets -n hiteen-prod

# ConfigMap 확인
kubectl get configmap -n hiteen-prod
```

---

## 🔧 문제 해결

### ImagePullBackOff 에러
```bash
# 원인: Registry 인증 실패
kubectl describe pod <POD_NAME> -n hiteen-prod

# 해결: Secret 재생성
kubectl delete secret gitlab-registry -n hiteen-prod
kubectl create secret docker-registry gitlab-registry \
  --docker-server=gitlab.barunsoft.net:6005 \
  --docker-username=<DEPLOY_TOKEN_USERNAME> \
  --docker-password=<DEPLOY_TOKEN> \
  -n hiteen-prod
```

### CrashLoopBackOff 에러
```bash
# 원인: 앱 시작 실패
kubectl logs <POD_NAME> -n hiteen-prod --previous

# 환경변수/설정 확인
kubectl describe pod <POD_NAME> -n hiteen-prod
```

### Pending 상태
```bash
# 원인: 리소스 부족 또는 PVC 미연결
kubectl describe pod <POD_NAME> -n hiteen-prod
kubectl get pvc -n hiteen-prod
```
