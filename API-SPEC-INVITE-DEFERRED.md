# HiTeen 초대코드(Invite) / 디퍼드 딥링크 API 명세

- Base URL: `https://dev.hiteen.co.kr`
- 인증: 명시되지 않은 엔드포인트는 **인증 불필요**. 일부는 로그인 시 추가 정보 기록을 위해 Authorization을 **선택적으로** 받을 수 있음.
- 공통 응답 래퍼: `ApiResult<T>`
  - 성공: `{ "success": true, "data": ..., "message": "..." }`
  - 실패: `{ "success": false, "message": "...", "errors": { ... } }`

---

## 1) 초대 링크(공유 URL)

### 1.1 초대 링크 열기 (랜딩 + 앱 오픈 시도 + 스토어 폴백)
- Method: `GET`
- Path: `/r/invite`
- Query
  - `code` (string, required): 초대코드

#### 동작
- 공통: 브라우저에서 **앱 설치 여부**에 따라 아래 흐름으로 동작합니다.
  - 설치됨: `hiteen://invite?code={code}` 로 앱 열기 시도
  - 미설치/실패:
    - **Android**: Play Store로 이동하며 `referrer=t=<token>`을 포함합니다(Install Referrer용).
    - **iOS**: App Store로 이동합니다.

> iOS는 유료 디퍼드 딥링크 SDK 미사용 기준으로 설치 후 자동 복원이 보장되지 않습니다.
> 따라서 랜딩 화면에서 `초대코드 복사` 버튼을 제공하며, 사용자는 가입 화면에서 붙여넣기할 수 있습니다.

#### Response
- `200 text/html`

#### 예시
- 링크 공유 예시
  - `https://dev.hiteen.co.kr/r/invite?code=TMUZTTCH6L`

---

## 2) Android 디퍼드(Install Referrer) 토큰 API

> Android는 Play Install Referrer로 설치 후 파라미터를 받을 수 있습니다.
> 서버는 보안을 위해 초대코드를 그대로 referrer에 싣지 않고, **1회용 토큰**을 발급해 전달합니다.

### 2.1 디퍼드 토큰 발급
- Method: `POST`
- Path: `/api/invite/deferred/issue`
- Auth: 없음
- Query
  - `code` (string, required): 초대코드

#### Response (성공)
```json
{
  "success": true,
  "data": {
    "token": "<url-safe-token>"
  },
  "message": null
}
```

#### 비고
- 일반적으로 앱/클라이언트가 직접 호출할 필요는 없고, `/r/invite`가 내부적으로 토큰을 발급합니다.

---

### 2.2 디퍼드 토큰 복원(토큰 → 초대코드)
- Method: `GET`
- Path: `/api/invite/deferred/resolve`
- Auth: 선택(로그인 되어 있으면 usedBy 기록용)
  - Header: `Authorization: Bearer <accessToken>` (optional)
- Query
  - `token` (string, required): Install Referrer에서 읽은 토큰

#### Response (성공)
```json
{
  "success": true,
  "data": {
    "code": "TMUZTTCH6L"
  },
  "message": null
}
```

#### Response (실패)
- 토큰 만료/이미 사용/존재하지 않음
```json
{
  "success": false,
  "data": null,
  "message": "invalid token",
  "errors": {
    "code": ["invalid token"]
  }
}
```

---

## 3) 회원가입 시 초대코드 적용

### 3.1 회원가입
- Method: `POST`
- Path: `/api/user`
- Content-Type: `multipart/form-data`
- Auth: 없음

#### Form fields (주요)
- `username` (string)
- `password` (string)
- `nickname` (string)
- `phone` (string)
- `inviteCode` (string, optional): 초대코드

> 서버는 `inviteCode`가 있으면 초대 처리 로직을 수행합니다.

#### Response
- `200 application/json` (`ApiResult<UserResponseWithTokens>`)

---

## 4) 클라이언트 구현 가이드(요약)

### Android (자동)
1. 사용자가 `https://dev.hiteen.co.kr/r/invite?code=...` 링크 클릭
2. 미설치라면 Play Store로 이동하면서 `referrer=t=<token>` 전달
3. 앱 설치 후 첫 실행에서 Install Referrer API로 `t=<token>` 읽기
4. `GET /api/invite/deferred/resolve?token=<token>` 호출 → `code` 획득
5. 회원가입 요청에 `inviteCode=<code>` 포함

### iOS (백업 UX)
- 설치되어 있으면 `hiteen://invite?code=...`로 앱이 열리며 `code`를 바로 사용
- 미설치라면 App Store로 이동(설치 후 자동 복원은 보장되지 않음)
  - 랜딩 화면에서 `초대코드 복사` → 설치 후 가입 화면에서 붙여넣기

