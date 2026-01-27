# perf-test (개발서버 성능 테스트)

개발서버(`dev.hiteen.co.kr`)에 **GET(read) 요청만** 보내서 성능을 측정하기 위한 폴더입니다.

- 테스트 도구: [k6](https://k6.io/)
- 주의: 이 폴더의 스크립트는 **쓰기(POST/PUT/PATCH/DELETE)** 를 하지 않습니다.
- 토큰(JWT) 같은 민감정보는 **파일에 하드코딩하지 말고** 환경변수로 주입하세요.

## 1) 준비

### k6 설치(macOS)
```bash
brew install k6
```

## 2) 투표 목록(GET /api/polls) 부하 테스트

### 실행 예시
```bash
export BASE_URL="https://dev.hiteen.co.kr"
export TOKEN="<JWT>"

k6 run perf-test/polls-list.js
```

### 옵션 바꾸기
- `VUS`(동시 사용자 수), `DURATION`(테스트 시간) 환경변수로 조절합니다.

```bash
export VUS=50
export DURATION=5m
k6 run perf-test/polls-list.js
```

## 3) Grafana에서 같이 보면 좋은 PromQL

### (1) 엔드포인트별 p95 latency Top 20
```promql
topk(20,
  histogram_quantile(0.95,
    sum by (le, uri, method) (
      rate(http_server_requests_seconds_bucket{job=~"hiteen.*", uri!~"/actuator.*"}[5m])
    )
  )
)
```

### (2) 엔드포인트별 총 소요시간(부하 기여) Top 20
```promql
topk(20,
  sum by (uri, method) (
    rate(http_server_requests_seconds_sum{job=~"hiteen.*", uri!~"/actuator.*"}[5m])
  )
)
```

### (3) WAS Pod별 CPU 사용량(코어)
> app label이 `hiteen-test`인 기준입니다. 다르면 정규식만 바꾸세요.
```promql
sum by (pod) (
  rate(container_cpu_usage_seconds_total{namespace="hiteen", pod=~"hiteen-test.*", container!="", image!=""}[5m])
)
```

### (4) WAS Pod별 메모리 사용량(working set, bytes)
```promql
sum by (pod) (
  container_memory_working_set_bytes{namespace="hiteen", pod=~"hiteen-test.*", container!="", image!=""}
)
```

