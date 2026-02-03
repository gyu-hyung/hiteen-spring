📌 Context: Asset Thumbnail Memory Spike Root Cause & Fix Guide
1. Incident Summary (What happened)

Kubernetes 환경에서 Spring Boot (WebFlux + Kotlin) 기반 API 서버가
09:37 전후 특정 Pod에서 메모리 사용량이 급격히 증가함

Pod 메모리:

약 250Mi → 770Mi (JVM Heap)

컨테이너 RSS는 ~1.7Gi까지 상승

Pod 재시작 없음 (OOMKill ❌)

트래픽 폭주 없음

2. Key Evidence (Prometheus / Grafana)
   2.1 Problematic API
   GET /api/assets/{uid}/view/{size}


(썸네일 조회 + 없으면 생성)

2.2 API Latency
sum(rate(http_server_requests_seconds_sum{uri="/api/assets/{uid}/view/{size}"}[5m]))
/
sum(rate(http_server_requests_seconds_count{uri="/api/assets/{uid}/view/{size}"}[5m]))


09:37 전후 평균 응답시간 ≈ 5초

→ 캐시 히트가 아닌 썸네일 신규 생성 발생

2.3 API RPS (Traffic)
sum by (uri, method)(
rate(http_server_requests_seconds_count{uri="/api/assets/{uid}/view/{size}"}[1m])
)


RPS ≈ 0.04

초당 요청 수 극히 적음

❌ 트래픽 폭주 아님

⭕ 단일 요청이 매우 무거움

2.4 JVM Heap Usage
sum by (pod) (jvm_memory_used_bytes{area="heap"})


Heap 사용량 급증:

250Mi → 770Mi

GC 후에도 힙이 내려오지 않고 유지됨

JVM 정상 동작 (메모리 릭 아님)

2.5 GC Pause
sum by (pod) (rate(jvm_gc_pause_seconds_sum[5m]))


이미지 처리 시점에 GC pause 급증

대량 객체 생성 / 힙 확장 패턴과 일치

3. Root Cause (확정)
   ❗ 핵심 원인

썸네일 생성 과정에서 고해상도 이미지가 “전체 디코드”되며 대량 메모리를 사용함

문제가 된 코드 패턴
val srcImage = readImage(sourcePath) // ImageIO reader.read(0)
Thumbnails.of(srcImage)


readImage() 내부에서 reader.read(0) 호출

이는 원본 이미지 모든 픽셀을 BufferedImage로 로딩

예:

4032x3024 (12MP) → 약 48MB (ARGB)

중간 버퍼 + 리사이즈 결과 + 인코딩 버퍼 포함 시
→ 요청 1건당 100~300MB 피크 가능

추가 악화 요인

Semaphore(permits = 2)

고해상도 이미지 2개 동시 처리 가능

메모리 피크 2배 상승 가능