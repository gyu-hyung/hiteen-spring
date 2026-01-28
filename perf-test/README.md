# perf-test (k6)

이 폴더는 서버 배포 전 **API 성능 측정/부하 테스트(k6)** 용 스크립트를 모아둡니다.

## 준비

- k6 설치: https://k6.io/docs/get-started/installation/
- JWT 토큰 준비

## 공통 환경변수

- `TOKEN` (필수): Bearer JWT (헤더에 `Authorization: Bearer <TOKEN>`)
- `BASE_URL` (선택): 기본값 `http://dev.hiteen.co.kr`
- `VUS` (선택): 가상 유저 수, 스크립트별 기본값 존재
- `DURATION` (선택): 실행 시간, 스크립트별 기본값 존재
- `LAT`, `LNG` (선택): pins/location 테스트용 좌표 (기본: 37.5666 / 127.0)

## 스크립트

### 1) `app-start-and-bottomnav.js`

앱 실행 시 호출되는 API + 바텀네비 각 메뉴의 list API 흐름을 시뮬레이션합니다.

호출되는 API:
- 앱 최초 진입
  - `POST /api/user-details`
  - `GET  /api/attend`
  - `GET  /api/pins?lat&lng&radius`
  - `GET  /api/friends`
  - `POST /api/location`
- 바텀네비
  - 틴스토리: `GET /api/boards`
  - 틴투표: `GET /api/polls`
  - 틴나우: `GET /api/friends`, `GET /api/pins`
  - 틴픽: `GET /api/interests/users/recommend/today`
  - 틴플레이: `GET /api/games`

실행 예시:
```bash
TOKEN="<JWT>" \
BASE_URL="http://dev.hiteen.co.kr" \
VUS=50 DURATION=5m \
LAT=37.5666 LNG=127.0 \
k6 run perf-test/app-start-and-bottomnav.js
```

### 2) `polls-list.js`

투표 목록 단일 엔드포인트만 부하 테스트합니다.

실행 예시:
```bash
TOKEN="<JWT>" BASE_URL="http://dev.hiteen.co.kr" VUS=150 DURATION=5m \
k6 run perf-test/polls-list.js
```

## 참고

- 실제 앱의 요청 파라미터(예: `pins`의 lat/lng)는 테스트 목적에 맞게 env로 조절하세요.
- `POST /api/user-details`는 서버 DTO가 대부분 optional 필드라 최소 payload로 호출합니다.
- 401/403이 뜨면 토큰 만료/권한(추천 API 등) 조건을 먼저 확인하세요.

