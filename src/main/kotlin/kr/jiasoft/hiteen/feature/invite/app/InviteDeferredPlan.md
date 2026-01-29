# 초대코드 Deferred Deep Link(설치 후 이어지게) – 서버/API 계약

## 목표
- Android/iOS 모두에서 "초대 링크 클릭 → (미설치면 설치) → 앱 첫 실행에서 초대코드가 이어짐"을 원함

## 결론(유료 SDK 미사용 기준)
- **Android: 가능(무료)** → Google Play **Install Referrer**로 설치 후 자동 복원 가능
- **iOS: 100% 자동 복원은 불가** → 유료 SDK(Branch/AppsFlyer 등) 없이 설치 후 파라미터 자동 복원 표준이 없음
  - 설치되어 있으면 Universal Link/App Scheme으로 code 전달 가능
  - 미설치 후에는 백업 UX 필요(예: 가입 화면에 수동 입력, 또는 웹에서 복사 버튼 제공 후 앱이 클립보드 감지 등)

> 따라서 서버는 "Android 자동 디퍼드" + "iOS는 설치된 경우 딥링크"를 지원하고,
> iOS 미설치 → 설치 후 자동 복원은 앱 UX로 보완하는 형태가 현실적인 최종안.

---

## 서버 엔드포인트(확정)

### 1) 공유 링크(설치 유도 + 앱 열기 시도)
- `GET /r/invite?code={INVITE_CODE}`
  - 앱이 설치되어 있으면 `hiteen://invite?code=...`로 앱 오픈 시도
  - 미설치/실패 시 스토어로 이동
    - Android: Play Store URL에 `referrer=t=<token>`을 붙여 이동
    - iOS: App Store URL로 이동

> Android에서 사용하는 token은 서버가 발급한 1회용 토큰이며, 앱이 설치 후 referrer에서 읽어 서버에서 code로 복원한다.

### 2) 디퍼드 토큰 발급/복원 API
- `POST /api/invite/deferred/issue?code={INVITE_CODE}`
  - 응답: `{ token }`
  - 보통은 `/r/invite` 내부에서 서버가 자동 발급 (앱/웹이 직접 호출할 일은 거의 없음)

- `GET /api/invite/deferred/resolve?token={TOKEN}`
  - 응답: `{ code }`
  - 토큰 만료/사용됨이면 400

---

## Android 앱 구현 계약(필수)
1. 설치 후 첫 실행 시 Install Referrer API로 `referrer` 문자열을 읽음
2. `t=<token>` 파싱
3. 서버 `GET /api/invite/deferred/resolve?token=...` 호출 → `code` 획득
4. 가입 화면으로 이동 + `inviteCode` 자동 입력
5. 회원가입 API에 `inviteCode` 포함

## iOS 앱 구현 계약(필수)
- 설치되어 있을 때: `hiteen://invite?code=...` (혹은 Universal Link) 수신 → 가입 화면에 자동 입력
- 미설치 후 설치: 자동 복원 보장 불가 → 가입 화면에 수동 입력 UI(백업) 필수

---

## 데이터/보안
- DB 테이블: `invite_link_tokens`
  - token(임의값), code, expires_at, used_at(1회 사용), used_by
- 토큰 기본 만료: 7일
- resolve 성공 시 used 처리(재사용 방지)
