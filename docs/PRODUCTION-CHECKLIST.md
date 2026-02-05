# 운영 환경 배포 체크리스트

운영 서버 배포 전 확인해야 할 체크리스트입니다.

---

## 📋 Phase 1: 인프라 준비

### 서버 준비
- [ ] 마스터 노드 서버 준비 (최소 4 CPU, 8GB RAM)
- [ ] 워커 노드 서버 준비 (최소 4 CPU, 16GB RAM x 2대)
- [ ] NFS 서버 준비 (최소 500GB 스토리지)
- [ ] 모든 서버 간 내부 네트워크 통신 확인
- [ ] 외부 접근용 공인 IP 또는 로드밸런서 준비

### 네트워크 설정
- [ ] 내부 IP 대역 확정 (예: 10.8.0.0/24)
- [ ] 방화벽 포트 개방 (6443, 10250, 30000-32767 등)
- [ ] DNS 레코드 설정 (api.hiteen.co.kr → Ingress IP)
- [ ] SSL 인증서 준비 또는 cert-manager 설정 계획

---

## 📋 Phase 2: K8s 클러스터 구축

### 모든 노드 공통
- [ ] OS 업데이트 완료
- [ ] 호스트명 설정
- [ ] /etc/hosts 설정
- [ ] Swap 비활성화
- [ ] 방화벽/SELinux 설정
- [ ] 커널 모듈 로드 (overlay, br_netfilter)
- [ ] 커널 파라미터 설정 (ip_forward 등)
- [ ] Containerd 설치 및 SystemdCgroup 활성화
- [ ] kubeadm, kubelet, kubectl 설치

### 마스터 노드
- [ ] kubeadm init 실행
- [ ] kubectl 설정 (~/.kube/config)
- [ ] Calico CNI 설치
- [ ] 노드 상태 확인 (kubectl get nodes)
- [ ] Join 토큰 저장

### 워커 노드
- [ ] kubeadm join 실행
- [ ] 노드 라벨 설정 (node-type=private)
- [ ] 모든 노드 Ready 상태 확인

### 필수 컴포넌트
- [ ] local-path-provisioner 설치
- [ ] ingress-nginx 설치
- [ ] metrics-server 설치
- [ ] Helm 설치

---

## 📋 Phase 3: NFS 설정

### NFS 서버
- [ ] nfs-utils/nfs-kernel-server 설치
- [ ] /srv/nfs/assets 디렉토리 생성
- [ ] /srv/nfs/backup 디렉토리 생성
- [ ] /etc/exports 설정
- [ ] exportfs -rav 실행
- [ ] 방화벽 NFS 포트 개방

### 워커 노드
- [ ] nfs-utils/nfs-common 설치
- [ ] NFS 마운트 테스트

---

## 📋 Phase 4: 인프라 배포 (hiteen-infra-chart)

### 사전 준비
- [ ] 네임스페이스 생성 (kubectl create ns hiteen-prod)
- [ ] Redis 비밀번호 결정
- [ ] DB 백업용 PostgreSQL 접속 정보 확인

### Helm 배포
```bash
helm upgrade --install hiteen-infra ./hiteen-infra-chart \
  -n hiteen-prod \
  --set redis.password=<REDIS_PASSWORD> \
  --set dbBackup.postgres.password=<DB_PASSWORD> \
  --set nfs.server=<NFS_IP> \
  --set backupNfs.server=<NFS_IP>
```

### 검증
- [ ] NFS PV/PVC 생성 확인
- [ ] Redis StatefulSet 6개 Pod Running
- [ ] Redis 클러스터 초기화 확인
  ```bash
  kubectl exec -n hiteen-prod redis-0 -- redis-cli -a <PASSWORD> cluster info
  kubectl exec -n hiteen-prod redis-0 -- redis-cli -a <PASSWORD> cluster nodes
  ```
- [ ] DB 백업 CronJob 생성 확인

---

## 📋 Phase 5: 애플리케이션 배포 (hiteen-app-chart)

### 사전 준비
- [ ] GitLab Registry Secret 생성
  ```bash
  kubectl create secret docker-registry gitlab-registry-secret \
    --docker-server=registry.gitlab.com \
    --docker-username=<USER> \
    --docker-password=<TOKEN> \
    -n hiteen-prod
  ```
- [ ] Firebase Secret 생성
  ```bash
  kubectl create secret generic firebase-secret \
    --from-file=firebase-key.json=/path/to/key.json \
    -n hiteen-prod
  ```
- [ ] secrets-prod.yaml 파일 준비
- [ ] Docker 이미지 빌드 및 Registry 푸시

### Helm 배포
```bash
helm upgrade --install hiteen-app ./hiteen-app-chart \
  -n hiteen-prod \
  -f ./hiteen-app-chart/values.yaml \
  -f ./secrets-prod.yaml \
  --set app.image.tag=<TAG>
```

### 검증
- [ ] Deployment Pod Running 확인
- [ ] Service 생성 확인
- [ ] Ingress 생성 확인
- [ ] HPA 생성 확인
- [ ] PDB 생성 확인
- [ ] Health Check 응답 확인
  ```bash
  kubectl exec -n hiteen-prod <POD> -- curl -s localhost:8080/actuator/health
  ```

---

## 📋 Phase 6: 네트워크 및 도메인

### Ingress 설정
- [ ] Ingress Controller NodePort 확인
  ```bash
  kubectl get svc -n ingress-nginx
  ```
- [ ] 외부 로드밸런서/방화벽에서 NodePort로 포워딩

### 도메인 설정
- [ ] DNS A 레코드 설정 (api.hiteen.co.kr → 공인 IP)
- [ ] SSL 인증서 설정 또는 cert-manager 확인
- [ ] HTTPS 접근 테스트

### 외부 접근 테스트
- [ ] curl https://api.hiteen.co.kr/actuator/health
- [ ] API 응답 확인

---

## 📋 Phase 7: 데이터 마이그레이션

### PostgreSQL
- [ ] 개발 DB에서 필수 테이블 덤프
  ```bash
  pg_dump -h <DEV_HOST> -U hiteen -d hiteen2-dev \
    -t api_keys -t assets -t codes ... \
    -F c -f hiteen-init-data.dump
  ```
- [ ] 운영 DB로 복원
  ```bash
  pg_restore -h <PROD_HOST> -U hiteen -d hiteen-prod \
    --clean --if-exists hiteen-init-data.dump
  ```
- [ ] 데이터 정합성 확인

### Assets (NFS)
- [ ] 개발 서버 assets 파일 복사
- [ ] 파일 권한 확인

---

## 📋 Phase 8: 모니터링

### Prometheus + Grafana
- [ ] kube-prometheus-stack 설치
  ```bash
  helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
  helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
  ```
- [ ] ServiceMonitor 동작 확인
- [ ] Grafana 대시보드 접근 확인
- [ ] 알림 설정 (Slack/Email)

### 로그 수집 (선택)
- [ ] Loki 또는 EFK 스택 설치
- [ ] 애플리케이션 로그 수집 확인

---

## 📋 Phase 9: CI/CD

### GitLab CI/CD
- [ ] .gitlab-ci.yml 푸시
- [ ] GitLab CI/CD 변수 설정
  - [ ] KUBE_CONFIG_PROD (base64 인코딩된 kubeconfig)
- [ ] 파이프라인 테스트 실행
- [ ] 수동 배포 승인 프로세스 확인

---

## 📋 Phase 10: 최종 검증

### 기능 테스트
- [ ] 사용자 회원가입/로그인
- [ ] 주요 API 엔드포인트 테스트
- [ ] WebSocket 연결 테스트
- [ ] 파일 업로드 테스트
- [ ] 푸시 알림 테스트

### 부하 테스트
- [ ] 동시 접속자 테스트
- [ ] API 응답 시간 측정
- [ ] HPA 스케일링 확인

### 장애 대응 테스트
- [ ] Pod 강제 종료 후 복구 확인
- [ ] Rolling Update 테스트
- [ ] Rollback 테스트
  ```bash
  helm rollback hiteen-app <REVISION> -n hiteen-prod
  ```

### 백업 테스트
- [ ] DB 백업 CronJob 수동 실행
  ```bash
  kubectl create job --from=cronjob/postgres-backup manual-backup -n hiteen-prod
  ```
- [ ] 백업 파일 확인
- [ ] 복원 테스트

---

## 📝 운영 정보 기록

완료 후 아래 정보를 문서화하세요:

```yaml
# 운영 환경 정보
cluster:
  master: 10.8.0.100
  workers:
    - 10.8.0.101
    - 10.8.0.102
  
nfs:
  server: 10.8.0.200
  paths:
    assets: /srv/nfs/assets
    backup: /srv/nfs/backup

database:
  host: 10.8.0.xxx
  port: 5432
  name: hiteen-prod

ingress:
  nodePort:
    http: 30080
    https: 30443
  
domain:
  api: api.hiteen.co.kr
  
monitoring:
  grafana: http://xxx:30300
  prometheus: http://xxx:30090
```

---

## 🆘 긴급 연락처

| 역할 | 담당자 | 연락처 |
|------|--------|--------|
| 인프라 담당 | | |
| 백엔드 개발 | | |
| DevOps | | |

---

**배포 완료 일시**: ____년 __월 __일 __시

**배포 담당자**: ________________

**승인자**: ________________

