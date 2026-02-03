# hiteen-monitoring-chart

Hiteen 개발/운영 k8s 환경에 **Prometheus + Grafana + Alertmanager**를 설치하기 위한 모니터링 전용 Helm 차트입니다.

- 모니터링 네임스페이스: `monitoring`
- Grafana 도메인: `monitor.dev.hiteen.co.kr`
- 기반 차트: `prometheus-community/kube-prometheus-stack`

## 빠른 시작 (명령어 모음)

    > 전제: 로컬/서버에 `kubectl`, `helm`이 설치돼 있고, 현재 Kube context가 대상 클러스터를 가리키고 있어야 합니다.

### 0) Helm repo 준비
```bash
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add bitnami https://charts.bitnami.com/bitnami
    helm repo update
```

### 1) 네임스페이스 생성 (이미 있으면 생략 가능)
```bash
    kubectl create namespace hiteen --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
```

### 2) 모니터링 차트 의존성 다운로드
```bash
    helm dependency update ./hiteen-monitoring-chart
```

### 3) 모니터링 스택 설치/업그레이드
```bash
  helm upgrade --install monitoring ./hiteen-monitoring-chart -n monitoring
```

### 4) (필수) Redis exporter 설치
Redis 대시보드/알럿은 exporter가 있어야 동작합니다.

> 아래는 Bitnami `redis-exporter`를 별도 릴리스로 설치하는 예시입니다.
> Redis 비밀번호를 secret에서 읽어 주입하도록 했습니다.

```bash
    REDIS_PASSWORD=$(kubectl -n hiteen get secret redis-secret -o jsonpath='{.data.redis-password}' | base64 --decode)
    
    helm upgrade --install redis-exporter bitnami/redis-exporter \
      -n monitoring \
      --set redisAddress=redis://redis.hiteen.svc.cluster.local:6379 \
      --set redisPassword="$REDIS_PASSWORD" \
      --set serviceMonitor.enabled=true \
      --set serviceMonitor.namespace=monitoring \
      --set serviceMonitor.labels.release=monitoring
```


### 5) Grafana 초기 비밀번호 확인
```bash
kubectl -n monitoring get secret monitoring-grafana -o jsonpath='{.data.admin-password}' | base64 --decode; echo
```

### 6) 상태 확인
```bash
kubectl -n monitoring get pods
kubectl -n monitoring get ingress

# Prometheus 타겟/서비스 확인
kubectl -n monitoring get servicemonitor
kubectl -n monitoring get prometheus
```

## Hiteen 앱 메트릭 연동 조건
- Spring Boot에서 `/actuator/prometheus` 노출 필요
- `ServiceMonitor`의 `metadata.labels.release`가 모니터링 릴리스명과 일치해야 합니다.

현재 레포는 `hiteen-chart/values.yaml`의 `metrics.serviceMonitor.releaseLabel: monitoring`로 맞춰져 있습니다.

## 포함된 기본 세트
- Grafana 대시보드(자동 프로비저닝)
  - Spring Boot(예시: JVM/Micrometer 기반)
  - Redis(Exporter 기반)
- Prometheus 알럿 룰(기본)
  - Hiteen 타겟 Down
  - Hiteen 5xx 에러율 상승
  - Hiteen p95 지연 증가
  - Redis exporter Down

## 트러블슈팅

### `helm: command not found`
Helm이 설치돼 있지 않습니다. (macOS 기준)
```bash
brew install helm
```

### Grafana Ingress가 안 뜸
- 클러스터에 Nginx Ingress Controller가 설치돼 있어야 합니다.
- 도메인 `monitor.dev.hiteen.co.kr`이 Ingress Controller로 라우팅되도록 DNS 설정이 필요합니다.

### Redis 대시보드가 비어있음
- `redis-exporter` 타겟이 Prometheus에서 UP인지 확인하세요.
- 아래 쿼리가 Prometheus에서 값이 나오면 exporter는 정상입니다:
  - `redis_up`
  - `redis_connected_clients`
