Admin 사용자 등록/수정 API 설계 지침

1. 목적 (Goal)
관리자(Admin)에서 회원 정보를 등록 및 수정하기 위한 API를 구현한다.
기존 AdminUserController.update 를 확장/수정하여 사용한다.
단순 users 테이블만 수정하는 것이 아니라, 연관된 여러 도메인(users / interests / photos / school / tier) 을 함께 일관성 있게 처리한다.

2. 핵심 도메인 구조 이해
   2.1 users (메인 엔티티)
회원의 기본 프로필 + 상태 정보를 담당
직접 관리 대상 필드:
기본 정보: username, email, nickname, phone
개인정보: gender, birthday, address, detail_address
상태/성향: mood, mood_emoji, mbti
시스템 필드:
school_id
class_id
tier_id
year
❗ school_id, class_id, tier_id 는 FK 관계이며, 단순 값 변경이 아닌 정합성 검증이 필요하다.

3. 연관 테이블 처리 규칙
   3.1 학교 / 반 / 티어 (N:1 관계)
   school / school_classes / tiers
users 테이블에는 다음 FK만 저장된다:
school_id
class_id
tier_id
검증 규칙
school_id가 존재할 경우:
schools 테이블에 실제 존재하는 id인지 확인
class_id가 존재할 경우:
school_classes 테이블에 존재하는 id인지 확인
가능하다면 users.school_id 와 school_classes.school_id 일치 검증
tier_id가 존재할 경우:
tiers 테이블에 존재
status = 'ACTIVE' 인 티어만 허용

3.2 관심사 (interest_user, 1:N)
사용자 ↔ 관심사는 중간 테이블 interest_user로 관리된다.
요청에서 관심사 ID 목록(interestIds)이 전달될 수 있다.
처리 전략 (권장)
수정 시:
기존 interest_user 레코드 전부 삭제
요청으로 받은 interestIds 기준으로 재삽입
UNIQUE 제약: (user_id, interest_id)
→ 중복 insert 방지 필요

3.3 사용자 사진 (user_photos, 1:N)
사진은 assets 테이블의 uid를 참조한다.
요청에서 photoUids 형태로 전달됨을 가정한다.
처리 전략
수정 시:
기존 user_photos 삭제
전달된 uid 목록 기준으로 재삽입
각 uid는 반드시:
assets 테이블에 존재해야 함
다른 user와 이미 매핑되지 않았는지 확인

4. API 동작 규칙
   4.1 트랜잭션
전체 작업은 단일 트랜잭션으로 처리
users / interest_user / user_photos 중 하나라도 실패하면 전체 롤백
4.2 수정(Update) 동작 원칙
null 값은 의미가 있을 때만 반영
예: 명시적으로 비우는 요청일 경우만 null 허용
존재하지 않는 user id → 404 또는 명확한 예외 처리
관리자(updated_id) 정보는 반드시 반영
4.3 생성(Create) vs 수정(Update)
id == null → 신규 사용자 생성
id != null → 기존 사용자 수정
신규 생성 시:
필수값 검증 강화
기본 role, tier, 상태값 세팅 고려

5. DTO 설계 가이드
   AdminUserSaveRequest (예시 필드)
users 직접 필드
username
email
nickname
phone
gender
birthday
schoolId
classId
tierId
year
연관 정보
interestIds: List<Long>
photoUids: List<UUID>
❗ Entity 직접 노출 금지
→ Controller → Service → Repository 계층 분리 유지

6. 예외 처리 기준
FK 대상 미존재 → IllegalArgumentException or BadRequest
UNIQUE 제약 위반 가능성 → 사전 체크
사용자 미존재 → NotFoundException
관리자 권한 미확인 → AccessDeniedException

7. 코딩 스타일 & 기술 스택
Kotlin + Spring WebFlux (suspend 함수)
Repository는 R2DBC 기반

8. 최종 목표
AdminUserController.update 하나로:
users
interest_user
user_photos
school / class / tier FK
를 일관성 있고 안전하게 관리할 수 있도록 구현한다.
🔥 이 컨텍스트의 의도
“단순 CRUD가 아니라
관리자 관점의 사용자 종합 편집 API를 정확히 구현하라”


9. 참고 : 테이블 스키마

CREATE TABLE public.users (
id bigserial NOT NULL,
uid uuid DEFAULT gen_random_uuid() NOT NULL,
username varchar(50) NULL,
email varchar(255) NULL,
nickname varchar(50) NULL,
"password" varchar(255) NULL,
"role" varchar(30) NULL,
address varchar(255) NULL,
detail_address varchar(255) NULL,
phone varchar(30) NULL,
mood varchar(30) NULL,
mood_emoji varchar(30) NULL,
mbti varchar(30) NULL,
exp_points int8 DEFAULT 0 NULL,
tier_id int8 NULL,
asset_uid uuid NULL,
school_id int8 NULL,
grade varchar(30) NULL,
gender varchar(30) NULL,
birthday date NULL,
profile_decoration_code varchar(50) NULL,
invite_code varchar(30) NULL,
invite_joins int8 NULL,
created_id int8 NULL,
created_at timestamptz DEFAULT now() NOT NULL,
updated_id int8 NULL,
updated_at timestamptz NULL,
deleted_id int8 NULL,
deleted_at timestamptz NULL,
class_id int8 NULL,
location_mode bool DEFAULT false NOT NULL,
"year" int2 NULL,
CONSTRAINT users_pkey PRIMARY KEY (id),
CONSTRAINT users_role_phone_key UNIQUE (role, phone),
CONSTRAINT fk_users_class_id FOREIGN KEY (class_id) REFERENCES public.school_classes(id) ON DELETE SET NULL,
CONSTRAINT users_asset_uid_fkey FOREIGN KEY (asset_uid) REFERENCES public.assets(uid),
CONSTRAINT users_tier_id_fkey FOREIGN KEY (tier_id) REFERENCES public.tiers(id)
);
CREATE INDEX idx_users_class_id ON public.users USING btree (class_id);
CREATE INDEX idx_users_location_mode ON public.users USING btree (location_mode);
CREATE INDEX users_role_key ON public.users USING btree (role);
CREATE UNIQUE INDEX users_username_key ON public.users USING btree (lower((username)::text)) WHERE (deleted_at IS NULL);
CREATE TABLE public.schools (
id bigserial NOT NULL,
sido varchar(20) NULL,
sido_name varchar(50) NULL,
code varchar(30) NULL,
"name" varchar(100) NULL,
"type" int4 NULL,
type_name varchar(30) NULL,
zipcode varchar(10) NULL,
address varchar(255) NULL,
latitude numeric(10, 7) NULL,
longitude numeric(10, 7) NULL,
coords text NULL,
found_date date NULL,
created_id int8 NULL,
created_at timestamptz DEFAULT now() NULL,
updated_id int8 NULL,
updated_at timestamptz NULL,
deleted_id int8 NULL,
deleted_at timestamptz NULL,
modified int2 DEFAULT '0'::smallint NULL,
CONSTRAINT schools_pkey PRIMARY KEY (id)
);


CREATE TABLE public.school_classes (
id bigserial NOT NULL,
code varchar(50) NULL,
"year" int2 NULL,
school_id int8 NULL,
school_name varchar(100) NULL,
school_type varchar(20) NULL,
class_name varchar(50) NULL,
major varchar(50) NULL,
grade varchar(10) NULL,
class_no varchar(100) NULL,
created_id int8 NULL,
created_at timestamptz DEFAULT now() NULL,
updated_id int8 NULL,
updated_at timestamptz NULL,
deleted_id int8 NULL,
deleted_at timestamptz NULL,
CONSTRAINT school_classes_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_school_classes ON public.school_classes USING btree (school_id, year, grade, class_no);

CREATE TABLE public.tiers (
id bigserial NOT NULL,
tier_code varchar(30) NOT NULL,
tier_name_kr varchar(50) NOT NULL,
division_no int2 NULL,
"level" int2 NULL,
rank_order int4 NULL,
status varchar(20) DEFAULT 'ACTIVE'::character varying NULL,
min_points int4 DEFAULT 0 NOT NULL,
max_points int4 DEFAULT 0 NOT NULL,
uid uuid DEFAULT gen_random_uuid() NULL,
created_at timestamptz DEFAULT now() NULL,
updated_at timestamptz NULL,
deleted_at timestamptz NULL,
CONSTRAINT tiers_pkey PRIMARY KEY (id),
CONSTRAINT tiers_tier_code_division_no_key UNIQUE (tier_code, division_no)
);





CREATE TABLE public.interest_user (
id bigserial NOT NULL,
interest_id int8 NOT NULL,
user_id int8 NOT NULL,
created_at timestamptz DEFAULT now() NULL,
updated_at timestamptz NULL,
CONSTRAINT interest_user_pkey PRIMARY KEY (id),
CONSTRAINT interest_user_user_id_interest_id_key UNIQUE (user_id, interest_id),
CONSTRAINT interest_user_interest_id_fkey FOREIGN KEY (interest_id) REFERENCES public.interests(id) ON DELETE CASCADE,
CONSTRAINT interest_user_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);


CREATE TABLE public.user_photos (
id bigserial NOT NULL,
user_id int8 NOT NULL,
uid uuid NOT NULL,
CONSTRAINT user_photos_pkey PRIMARY KEY (id),
CONSTRAINT user_photos_uid_key UNIQUE (uid),
CONSTRAINT user_photos_user_id_uid_key UNIQUE (user_id, uid),
CONSTRAINT user_photos_uid_fkey FOREIGN KEY (uid) REFERENCES public.assets(uid),
CONSTRAINT user_photos_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE
);

