# Web API 문서 - Article & Inquiry

## 개요

웹에서 사용하는 공지사항/이벤트(Article) 및 문의하기(Inquiry) API 문서입니다.

---

## 🔗 Swagger API 문서 및 테스트 링크

### Swagger UI
| 환경     | URL |
|--------|-----|
| **링크** | https://api.hiteen.kr/swagger-ui/index.html |


### API URL

| 환경 | URL |
|------|-----|
| **운영** | `https://api.hiteen.kr` |

---

## 📌 Article (공지사항/이벤트) API

### 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `/api/articles` |
| 인증 | Bearer Token (선택) |

### 카테고리

| 값 | 설명 |
|----|------|
| `NOTICE` | 공지사항 |
| `EVENT` | 이벤트 |

### 상태 (이벤트용)

| 값 | 설명 |
|----|------|
| `ACTIVE` | 진행중 |
| `ENDED` | 종료됨 |
| `WINNING` | 당첨자 발표 |

---

### 1. 공지사항/이벤트 목록 조회 (페이지 기반)

**GET** `/api/articles`

#### Request Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `category` | String | ❌ | - | 카테고리 (`NOTICE` / `EVENT`) |
| `status` | String | ❌ | - | 상태 (`ACTIVE` / `ENDED` / `WINNING`) |
| `q` | String | ❌ | - | 검색어 (제목, 내용 검색) |
| `page` | Integer | ❌ | 0 | 페이지 번호 (0부터 시작) |
| `size` | Integer | ❌ | 20 | 페이지당 개수 |

#### Request Example

```http
GET /api/articles?category=EVENT&status=ACTIVE&page=0&size=10
Authorization: Bearer {token}
```

#### Response Example

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "category": "NOTICE",
        "subject": "서비스 점검 안내",
        "content": "2025년 9월 20일 점검 예정입니다.",
        "link": null,
        "hits": 125,
        "attachments": [
          "550e8400-e29b-41d4-a716-446655441111"
        ],
        "largeBanners": null,
        "smallBanners": null,
        "startDate": "2025-09-01",
        "endDate": "2025-09-30",
        "status": "ACTIVE",
        "createdAt": "2025.09.18 10:15",
        "updatedAt": "2025.09.18 10:15"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 50,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

### 2. 공지사항/이벤트 목록 조회 (커서 기반)

**GET** `/api/articles/cursor`

무한 스크롤에 적합한 커서 기반 페이지네이션입니다.

#### Request Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `category` | String | ❌ | - | 카테고리 (`NOTICE` / `EVENT`) |
| `status` | String | ❌ | - | 상태 (`ACTIVE` / `ENDED` / `WINNING`) |
| `q` | String | ❌ | - | 검색어 (제목, 내용 검색) |
| `size` | Integer | ❌ | 20 | 조회 개수 |
| `cursor` | Long | ❌ | - | 마지막 article id (다음 페이지 조회용) |

#### Request Example

```http
GET /api/articles/cursor?category=EVENT&status=WINNING&size=20&cursor=100
Authorization: Bearer {token}
```

#### Response Example

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "category": "EVENT",
        "subject": "여름 이벤트",
        "content": "여름맞이 특별 이벤트!",
        "link": "https://event.hiteen.kr",
        "hits": 350,
        "attachments": null,
        "largeBanners": [
          "550e8400-e29b-41d4-a716-446655440001"
        ],
        "smallBanners": [
          "550e8400-e29b-41d4-a716-446655440002"
        ],
        "startDate": "2025-07-01",
        "endDate": "2025-08-31",
        "status": "ACTIVE",
        "createdAt": "2025.06.20 14:30",
        "updatedAt": null
      }
    ],
    "nextCursor": 99,
    "hasNext": true
  }
}
```

---

### 3. 공지사항/이벤트 단건 조회

**GET** `/api/articles/{id}`

조회 시 조회수(hits)가 1 증가합니다. **이전글/다음글 정보를 포함**합니다.

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `id` | Long | ✅ | 게시글 ID |

#### Request Example

```http
GET /api/articles/1
```

#### Response Example

```json
{
  "success": true,
  "data": {
    "category": "NOTICE",
    "subject": "서비스 이용약관 변경 안내",
    "content": "안녕하세요. 이용약관이 변경되었습니다...",
    "link": null,
    "hits": 126,
    "attachments": [
      "550e8400-e29b-41d4-a716-446655441111",
      "550e8400-e29b-41d4-a716-446655441112"
    ],
    "largeBanners": null,
    "smallBanners": null,
    "startDate": null,
    "endDate": null,
    "status": "ACTIVE",
    "createdAt": "2025.09.01 09:00",
    "updatedAt": "2025.09.10 11:30",
    "prevArticle": {
      "id": 2,
      "subject": "이전 공지사항 제목",
      "createdAt": "2025.11.24"
    },
    "nextArticle": {
      "id": 5,
      "subject": "다음 공지사항 제목",
      "createdAt": "2025.11.26"
    }
  }
}
```

#### 이전글/다음글 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `prevArticle` | Object \| null | 이전글 정보 (없으면 null) |
| `prevArticle.id` | Long | 이전글 ID |
| `prevArticle.subject` | String | 이전글 제목 |
| `prevArticle.createdAt` | String | 이전글 등록일시 (yyyy.MM.dd) |
| `nextArticle` | Object \| null | 다음글 정보 (없으면 null) |
| `nextArticle.id` | Long | 다음글 ID |
| `nextArticle.subject` | String | 다음글 제목 |
| `nextArticle.createdAt` | String | 다음글 등록일시 (yyyy.MM.dd) |

> **참고**: 이전글/다음글은 **같은 카테고리** 내에서만 조회됩니다. (공지사항은 공지사항끼리, 이벤트는 이벤트끼리)
```

---

### 📎 첨부파일 조회

Article의 첨부파일(`attachments`, `largeBanners`, `smallBanners`)은 **Asset UID**로 제공됩니다.

실제 파일을 조회하려면 아래 Asset API를 사용하세요:

```
GET /api/assets/{assetUid}/view
```

#### 예시

```http
# Article 응답에서 받은 첨부파일 UID
"attachments": ["550e8400-e29b-41d4-a716-446655441111"]

# 해당 파일 조회
GET /api/assets/550e8400-e29b-41d4-a716-446655441111/view
```

#### 이미지 태그에서 사용

```html
<img src="https://api.hiteen.kr/api/assets/550e8400-e29b-41d4-a716-446655441111/view" alt="첨부 이미지" />
```

---

### Article Response 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `category` | String | 카테고리 (`NOTICE` / `EVENT`) |
| `subject` | String | 제목 |
| `content` | String | 내용 |
| `link` | String | 외부 링크 URL |
| `hits` | Integer | 조회수 |
| `attachments` | List<UUID> | 일반 첨부파일 UID 목록 (공지사항용) |
| `largeBanners` | List<UUID> | 큰 배너 이미지 UID 목록 (이벤트용) |
| `smallBanners` | List<UUID> | 작은 배너 이미지 UID 목록 (이벤트용) |
| `startDate` | LocalDate | 게시 시작일 |
| `endDate` | LocalDate | 게시 종료일 |
| `status` | String | 상태 (`ACTIVE` / `INACTIVE`) |
| `createdAt` | DateTime | 작성 일시 |
| `updatedAt` | DateTime | 수정 일시 |

---

## 📌 Inquiry (문의하기) API

### 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `/api/inquiry` |
| 인증 | 불필요 (Public) |

---

### 1. 문의하기 등록

**POST** `/api/inquiry`

웹에서 비로그인 상태로 문의를 등록합니다.

#### Request Body

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | String | ✅ | 이름 |
| `phone` | String | ✅ | 전화번호 |
| `email` | String | ❌ | 이메일 |
| `content` | String | ✅ | 문의 내용 |

#### Request Example

```http
POST /api/inquiry
Content-Type: application/json

{
  "name": "홍길동",
  "phone": "010-1234-5678",
  "email": "test@example.com",
  "content": "서비스 이용 관련하여 문의드립니다."
}
```

#### Response Example

```json
{
  "success": true,
  "data": {
    "id": 1
  }
}
```

---

### Inquiry Status (문의 상태)

| 값 | 설명 |
|----|------|
| `PENDING` | 대기중 (답변 전) |
| `REPLIED` | 답변 완료 |
| `CLOSED` | 종료 |

---

## 🔒 에러 응답

### 공통 에러 형식

```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "해당 게시글을 찾을 수 없습니다."
  }
}
```

### 주요 에러 코드

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `NOT_FOUND` | 404 | 리소스를 찾을 수 없음 |
| `BAD_REQUEST` | 400 | 잘못된 요청 파라미터 |
| `UNAUTHORIZED` | 401 | 인증 필요 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## 📋 요약

### Article API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/articles` | 목록 조회 (페이지) | 선택 |
| GET | `/api/articles/cursor` | 목록 조회 (커서) | 선택 |
| GET | `/api/articles/{id}` | 단건 조회 | 선택 |

### Inquiry API

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/api/inquiry` | 문의 등록 | 불필요 |

### Asset API (첨부파일 조회)

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| GET | `/api/assets/{assetUid}/view` | 파일 조회/다운로드 | 불필요 |


