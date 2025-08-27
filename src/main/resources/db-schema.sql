-- ========================
-- hiteen DDL
-- ========================

--CREATE EXTENSION IF NOT EXISTS pgcrypto; -- for gen_random_uuid()
--CREATE EXTENSION IF NOT EXISTS citext;   -- case-insensitive text



-- ========================
-- 사용자
-- ========================
CREATE TABLE users (
  id            bigserial PRIMARY KEY,
  uid           uuid        NOT NULL DEFAULT gen_random_uuid(),
  username      varchar(50),
  email         varchar(255),
  nickname      varchar(50),
  password      varchar(255),
  role          varchar(30),
  address       varchar(255),
  detail_address varchar(255),
  telno         varchar(30),
  mood          varchar(30),
  tier          varchar(30),
  asset_uid     uuid, -- REFERENCES assets(uid),
  created_id    bigint,
  created_at    timestamptz not null DEFAULT now(),
  updated_id    bigint,
  updated_at    timestamptz,
  deleted_id    bigint,
  deleted_at    timestamptz
);

-- 이메일: 삭제되지 않은 사용자만 유니크, 대소문자 구분 없음
CREATE UNIQUE INDEX users_email_key
    ON users (lower(email))
    WHERE deleted_at IS NULL;

-- 사용자명: 삭제되지 않은 사용자만 유니크, 대소문자 구분 없음
CREATE UNIQUE INDEX users_username_key
    ON users (lower(username))
    WHERE deleted_at IS NULL;


-- ========================
-- 코드
-- ========================
CREATE TABLE codes (
  id              bigserial PRIMARY KEY,
  code_name       varchar(100) NOT NULL,
  code            varchar(50)  NOT NULL,
  code_group_name varchar(100), -- 관심사 등
  code_group      varchar(50),
  status          varchar(20),
  created_id      bigint,
  created_at      timestamptz DEFAULT now(),
  updated_id      bigint,
  updated_at      timestamptz,
  deleted_id      bigint,
  deleted_at      timestamptz,
  UNIQUE (code_group, code)
);


-- ========================
-- 관심사
-- ========================
CREATE TABLE interests (
  id         bigserial PRIMARY KEY,
  topic      varchar(100) NOT NULL,
  category   varchar(50),
  status     varchar(20),
  created_id bigint,
  created_at timestamptz DEFAULT now(),
  updated_id bigint,
  updated_at timestamptz,
  deleted_id bigint,
  deleted_at timestamptz
);


-- ========================
-- 사용자-관심사 매핑
-- ========================
CREATE TABLE interest_user (
  id          bigserial PRIMARY KEY,
  interest_id bigint NOT NULL REFERENCES interests(id) ON DELETE CASCADE,
  user_id     bigint NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
  created_at  timestamptz DEFAULT now(),
  updated_at  timestamptz,
  UNIQUE (user_id, interest_id)
);


-- ========================
-- 자산(파일/이미지)
-- ========================
CREATE TABLE assets (
  id               bigserial PRIMARY KEY,
  uid              uuid        NOT NULL DEFAULT gen_random_uuid(),
  origin_file_name varchar(255),
  store_file_name   varchar(255),
  file_path        varchar(500),
  type             varchar(50),
  size             bigint,        -- bytes
  width            integer,
  height           integer,
  ext              varchar(10),
  download_count   integer        DEFAULT 0,
  created_id       bigint,
  created_at       timestamptz    DEFAULT now(),
  updated_id       bigint,
  updated_at       timestamptz,
  deleted_id       bigint,
  deleted_at       timestamptz,
  UNIQUE (uid)
);


-- ========================
-- 티어
-- ========================
CREATE TABLE tiers (
  id          bigserial PRIMARY KEY,
  tier_code   varchar(30) NOT NULL,
  division_no smallint,
  rank_order  integer,
  status      varchar(20),
  max_points  integer,
  min_points  integer,
  uid         uuid DEFAULT gen_random_uuid(),
  created_id  bigint,
  created_at  timestamptz DEFAULT now(),
  updated_id  bigint ,
  updated_at  timestamptz,
  deleted_id  bigint ,
  deleted_at  timestamptz,
  UNIQUE (tier_code)
);


-- ========================
-- 학교
-- ========================
CREATE TABLE schools (
  id          bigserial PRIMARY KEY,
  sido        varchar(20),
  sido_name   varchar(50),
  code        varchar(30),
  name        varchar(100),
  type        varchar(20),
  type_name   varchar(30),
  zipcode     varchar(10),
  address     varchar(255),
  latitude    numeric(9,6),
  longitude   numeric(9,6),
  coords      text,
  created_id  bigint ,
  created_at  timestamptz DEFAULT now(),
  updated_id  bigint ,
  updated_at  timestamptz,
  deleted_id  bigint ,
  deleted_at  timestamptz
);


-- ========================
-- 학급
-- ========================
CREATE TABLE classes (
  id           bigserial PRIMARY KEY,
  code         varchar(50),
  year         smallint,
  school_id    bigint REFERENCES schools(id),
  school_name  varchar(100),
  school_type  varchar(20),
  class_name   varchar(50),
  major        varchar(50),
  grade        varchar(10),
  class        varchar(10),
  created_id   bigint ,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz
);


-- ========================
-- 회원 학급정보
-- ========================
CREATE TABLE user_classes (
  id           bigserial PRIMARY KEY,
  user_id      bigint  NOT NULL REFERENCES users(id)   ON DELETE CASCADE,
  year         smallint,
  school_id    bigint  REFERENCES schools(id),
  class_id     bigint  REFERENCES classes(id),
  school_type  varchar(20),
  school_name  varchar(100),
  class_name   varchar(50),
  grade        varchar(10),
  auth_school  boolean,
  auth_class   boolean,
  created_id   bigint ,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz,
  UNIQUE (user_id, class_id)
);


-- ========================
-- 위치 핀
-- ========================
CREATE TABLE pin (
  id           bigserial PRIMARY KEY,
  user_id      bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  zipcode      varchar(10),
  lat          numeric(9,6),
  lng          numeric(9,6),
  description  varchar(255),
  type         varchar(30),
  created_id   bigint,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz
);


-- ========================
--사용자 참여
-- ========================
CREATE TABLE pin_users (
  id         bigserial PRIMARY KEY,
  pin_id     bigint   NOT NULL REFERENCES pin(id)   ON DELETE CASCADE,
  user_id    bigint   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at timestamptz DEFAULT now(),
  deleted_at timestamptz,
  UNIQUE (pin_id, user_id)
);


-- ========================
-- 신고
-- ========================
CREATE TABLE report (
  id         bigserial PRIMARY KEY,
  code       varchar(50),
  content    text,
  status     varchar(20),
  created_id bigint ,
  created_at timestamptz DEFAULT now(),
  updated_id bigint ,
  updated_at timestamptz,
  deleted_id bigint ,
  deleted_at timestamptz
);


-- ========================
-- 친구
-- ========================
CREATE TABLE friends (
  id         bigserial PRIMARY KEY,
  user_id    bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  friend_id  bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status     varchar(20),
  status_at  timestamptz,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  CONSTRAINT friends_not_self CHECK (user_id <> friend_id),
  UNIQUE (user_id, friend_id)
);

--CREATE INDEX idx_friends_user ON friends(user_id);
--CREATE INDEX idx_friends_friend ON friends(friend_id);
--CREATE INDEX idx_friends_status_user ON friends(status, user_id);
--CREATE INDEX idx_friends_status_friend ON friends(status, friend_id);

-- ========================
--팔로우
-- ========================
CREATE TABLE follows (
  id         bigserial PRIMARY KEY,
  user_id    bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  follow_id  bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  status     varchar(20),
  status_at  timestamptz,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz,
  deleted_at timestamptz,
  CONSTRAINT follows_not_self CHECK (user_id <> follow_id),
  UNIQUE (user_id, follow_id)
);


-- ========================
-- 팔로우 이력
-- ========================
CREATE TABLE follow_histories (
  id      bigserial PRIMARY KEY,
  user_id bigint REFERENCES users(id) ON DELETE SET NULL,
  field   varchar(255),
  created_at timestamptz DEFAULT now()
);


-- ========================
-- 게시판
-- ========================
CREATE TABLE boards (
  id             bigserial PRIMARY KEY,
  uid            uuid DEFAULT gen_random_uuid(),
  category       varchar(50),
  subject        varchar(200),
  content        text,
  link           varchar(500),
  ip             inet,
  hits           integer DEFAULT 0,
  asset_uid      uuid, -- REFERENCES assets(uid),
  start_date     date,
  end_date       date,
  report_count   integer DEFAULT 0,
  status         varchar(20),
  address        varchar(255),
  detail_address varchar(255),
  created_id     bigint ,
  created_at     timestamptz DEFAULT now(),
  updated_id     bigint ,
  updated_at     timestamptz,
  deleted_id     bigint ,
  deleted_at     timestamptz
);


-- ========================
-- 게시판 > 파일
-- ========================
CREATE TABLE board_assets (
  id        bigserial PRIMARY KEY,
  board_id  bigint NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  uid       uuid NOT NULL,
  UNIQUE (board_id, uid)
);


-- ========================
-- 게시판 > 댓글
-- ========================
CREATE TABLE board_comments (
  id           bigserial PRIMARY KEY,
  board_id     bigint NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  uid          uuid DEFAULT gen_random_uuid(),
  parent_id    bigint REFERENCES board_comments(id) ON DELETE CASCADE,
  content      text,
  reply_count  integer DEFAULT 0,
  report_count integer DEFAULT 0,
  created_id   bigint not null,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz
);


-- ========================
-- 게시판 > 좋아요
-- ========================
CREATE TABLE board_likes (
  id         bigserial PRIMARY KEY,
  board_id   bigint NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
  user_id    bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  created_at timestamptz DEFAULT now(),
--  updated_at timestamptz,
--  deleted_at timestamptz,
  UNIQUE (board_id, user_id)
);


-- ========================
-- 게시판 > 댓글 > 좋아요
-- ========================
CREATE TABLE board_comment_likes (
  id         bigserial PRIMARY KEY,
  comment_id bigint NOT NULL REFERENCES board_comments(id) ON DELETE CASCADE,
  user_id    bigint NOT NULL REFERENCES users(id)          ON DELETE CASCADE,
  created_at timestamptz DEFAULT now(),
--  updated_at timestamptz,
--  deleted_at timestamptz,
  UNIQUE (comment_id, user_id)
);


--CREATE UNIQUE INDEX IF NOT EXISTS ux_boards_uid ON boards(uid);
--CREATE INDEX IF NOT EXISTS ix_boards_created_at ON boards(created_at DESC);
--CREATE INDEX IF NOT EXISTS ix_boards_category ON boards(category);
--CREATE INDEX IF NOT EXISTS ix_bc_board_id ON board_comments(board_id);
--CREATE INDEX IF NOT EXISTS ix_bc_parent_id ON board_comments(parent_id);
--CREATE UNIQUE INDEX IF NOT EXISTS ux_board_likes ON board_likes(board_id, user_id);
--CREATE UNIQUE INDEX IF NOT EXISTS ux_comment_likes ON board_comment_likes(comment_id, user_id);



-- ========================
-- 투표
-- ========================
CREATE TABLE polls (
  id             bigserial PRIMARY KEY,
  question       varchar(255),
  photo          varchar(255),
  vote_count	   integer DEFAULT 0,
  comment_count  integer DEFAULT 0,
  report_count   integer DEFAULT 0,
  status         varchar(20),
  reply_at       timestamptz,
  created_id     bigint ,
  created_at     timestamptz DEFAULT now(),
  updated_id     bigint ,
  updated_at     timestamptz,
  deleted_id     bigint ,
  deleted_at     timestamptz
);


-- ========================
-- 투표 > 항목
-- ========================
CREATE TABLE poll_items (
  id       bigserial PRIMARY KEY,
  poll_id  bigint   NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
  seq      smallint NOT NULL,
  answer   varchar(255) NOT NULL,
  votes    integer DEFAULT 0,
  UNIQUE (poll_id, seq)
);


-- ========================
-- 투표 > 회원
-- ========================
CREATE TABLE poll_users (
  id        bigserial PRIMARY KEY,
  poll_id   bigint NOT NULL REFERENCES polls(id)  ON DELETE CASCADE,
  user_id   bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  voted_at  timestamptz DEFAULT now(),
  UNIQUE (poll_id, user_id)
);


-- ========================
-- 투표 > 댓글
-- ========================
CREATE TABLE poll_comments (
  id           bigserial PRIMARY KEY,
  poll_id      bigint NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
  uid          uuid DEFAULT gen_random_uuid(),
  parent_id    bigint REFERENCES poll_comments(id) ON DELETE CASCADE,
  content      text,
  reply_count  integer DEFAULT 0,
  report_count integer DEFAULT 0,
  created_id   bigint ,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz
);


-- ========================
-- 투표 > 댓글 > 좋아요
-- ========================
CREATE TABLE poll_comment_likes (
  id              bigserial PRIMARY KEY,
  poll_comment_id bigint NOT NULL REFERENCES poll_comments(id) ON DELETE CASCADE,
  user_id         bigint NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
  created_at      timestamptz DEFAULT now(),
  updated_at      timestamptz,
  deleted_at      timestamptz,
  UNIQUE (poll_comment_id, user_id)
);


-- ========================
-- 채팅방
-- ========================
CREATE TABLE chat_rooms (
  id              bigserial PRIMARY KEY,
  uid             uuid DEFAULT gen_random_uuid(),
  last_user_id    bigint ,
  last_message_id bigint, -- REFERENCES chat_messages(id), -- 후술 FK 추가 예정
  created_id   bigint ,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz
);


-- ========================
-- 채팅 > 참여자
-- ========================
CREATE TABLE chat_users (
  id          bigserial PRIMARY KEY,
  chat_room_id bigint NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
  user_id     bigint NOT NULL REFERENCES users(id)       ON DELETE CASCADE,
  status      smallint,    -- 예: 0=정상, 1=뮤트, 2=차단...
  push        boolean,
  push_at     timestamptz,
  joining_at  timestamptz,
  leaving_at  timestamptz,
  deleted_at  timestamptz,
  UNIQUE (chat_room_id, user_id)
);


-- ========================
-- 채팅 > 메세지
-- ========================
CREATE TABLE chat_messages (
  id           bigserial PRIMARY KEY,
  chat_room_id bigint NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
  user_id      bigint NOT NULL REFERENCES users(id)       ON DELETE CASCADE,
  uid          uuid  NOT NULL DEFAULT gen_random_uuid(),
  content      text,
  read_count   integer DEFAULT 0,
  created_at   timestamptz DEFAULT now(),
  updated_at   timestamptz,
  deleted_at   timestamptz
);


-- 이제 chat_rooms.last_message_id FK 연결(순환 참조 방지 위해 나중에 추가)
ALTER TABLE chat_rooms
  ADD CONSTRAINT chat_rooms_last_msg_fk
  FOREIGN KEY (last_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL;


CREATE TABLE chat_messages_assets (
  id         bigserial PRIMARY KEY,
  uid        uuid NOT NULL,
--  uid        uuid NOT NULL DEFAULT gen_random_uuid(),
  message_id bigint NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
  width      integer,
  height     integer
);


-- ========================
-- 포인트
-- ========================
CREATE TABLE point_rules (
  id           bigserial PRIMARY KEY,
  action_code  varchar(50)  NOT NULL,
  point  integer      NOT NULL,
  daily_cap    integer,
  cooldown_sec integer,
  created_id   bigint ,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz,
  UNIQUE (action_code)
);


-- ========================
-- 획득점수 이력
-- ========================
CREATE TABLE point_histories (
  id         bigserial PRIMARY KEY,
  user_id    bigint   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  point      integer  NOT NULL,
  meta_json  jsonb,
  created_at timestamptz DEFAULT now()
);


-- ========================
-- 유저 사진
-- ========================
CREATE TABLE user_photos (
  id      bigserial PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  uid     uuid NOT NULL,
--  uid     uuid   NOT NULL DEFAULT gen_random_uuid(),
  UNIQUE (uid),
  UNIQUE (user_id, uid)
);


-- ========================
-- MQTT 인증정보
-- ========================
--CREATE TABLE mqtt_credentials (
--    id bigserial PRIMARY KEY,
--    user_uid bigint not null references users(id) on delete cascade,
--    credentials_id VARCHAR(255) NOT NULL,
--    client_id VARCHAR(255) NOT NULL,
--    username VARCHAR(255) NOT NULL,
--    password VARCHAR(255) NOT NULL,
--    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
--    expires_at TIMESTAMP WITH TIME ZONE NULL
--);


-- ========================
-- 위치 이력_mongoDB
-- ========================
--CREATE TABLE location_histories (
--  id         bigserial PRIMARY KEY,
--  user_id    bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--  lat        numeric(9,6),
--  lng        numeric(9,6),
--  "timestamp" timestamptz,  -- 예약어 충돌 방지용 쿼트
--  created_at timestamptz DEFAULT now()
--);


--===========================================drop==================================================


--DROP TABLE IF EXISTS
--  user_photos,
--  point_histories,
--  point_rules,
--  chat_messages_assets,
--  chat_messages,
--  chat_users,
--  chat_rooms,
--  poll_comment_likes,
--  poll_comments,
--  poll_users,
--  poll_items,
--  polls,
--  board_comment_likes,
--  board_likes,
--  board_comments,
--  board_assets,
--  boards,
--  follow_histories,
--  follows,
--  friends,
--  report,
--  pin_users,
--  pin,
--  user_classes,
--  classes,
--  schools,
--  tiers,
--  assets,
--  interest_user,
--  interests,
--  codes,
--  users
--CASCADE;


--===========================================comment==================================================


-- ========================
-- users
-- ========================
COMMENT ON TABLE users IS '사용자 정보';
COMMENT ON COLUMN users.id IS '자동 증가 기본키';
COMMENT ON COLUMN users.uid IS 'UUID, 외부 노출용 고유 식별자';
COMMENT ON COLUMN users.username IS '로그인 아이디(고유)';
COMMENT ON COLUMN users.email IS '이메일(고유)';
COMMENT ON COLUMN users.nickname IS '닉네임';
COMMENT ON COLUMN users.password IS '비밀번호 해시';
COMMENT ON COLUMN users.role IS '권한(예: ADMIN/USER)';
COMMENT ON COLUMN users.address IS '주소';
COMMENT ON COLUMN users.detail_address IS '상세 주소';
COMMENT ON COLUMN users.telno IS '전화번호';
COMMENT ON COLUMN users.mood IS '사용자 상태/기분 코드';
COMMENT ON COLUMN users.tier IS '티어 코드 문자열';
COMMENT ON COLUMN users.asset_uid IS '프로필 썸네일 Asset UID(assets.uid)';
COMMENT ON COLUMN users.created_id IS '생성자 사용자 ID(감사용)';
COMMENT ON COLUMN users.created_at IS '생성 일시';
COMMENT ON COLUMN users.updated_id IS '수정자 사용자 ID(감사용)';
COMMENT ON COLUMN users.updated_at IS '수정 일시';
COMMENT ON COLUMN users.deleted_id IS '삭제자 사용자 ID(감사용)';
COMMENT ON COLUMN users.deleted_at IS '삭제 일시';

-- ========================
-- codes
-- ========================
COMMENT ON TABLE codes IS '공통 코드';
COMMENT ON COLUMN codes.id IS '자동 증가 기본키';
COMMENT ON COLUMN codes.code_name IS '코드명';
COMMENT ON COLUMN codes.code IS '코드 값';
COMMENT ON COLUMN codes.code_group_name IS '코드 그룹명(표시용)';
COMMENT ON COLUMN codes.code_group IS '코드 그룹 키';
COMMENT ON COLUMN codes.status IS '상태(예: 사용/중지)';
COMMENT ON COLUMN codes.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN codes.created_at IS '생성 일시';
COMMENT ON COLUMN codes.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN codes.updated_at IS '수정 일시';
COMMENT ON COLUMN codes.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN codes.deleted_at IS '삭제 일시';

-- ========================
-- interests
-- ========================
COMMENT ON TABLE interests IS '관심사 마스터';
COMMENT ON COLUMN interests.id IS '자동 증가 기본키';
COMMENT ON COLUMN interests.topic IS '관심사 주제';
COMMENT ON COLUMN interests.category IS '관심사 카테고리';
COMMENT ON COLUMN interests.status IS '상태';
COMMENT ON COLUMN interests.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN interests.created_at IS '생성 일시';
COMMENT ON COLUMN interests.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN interests.updated_at IS '수정 일시';
COMMENT ON COLUMN interests.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN interests.deleted_at IS '삭제 일시';

-- ========================
-- interest_user
-- ========================
COMMENT ON TABLE interest_user IS '사용자와 관심사 매핑';
COMMENT ON COLUMN interest_user.id IS '자동 증가 기본키';
COMMENT ON COLUMN interest_user.interest_id IS '관심사 ID(FK)';
COMMENT ON COLUMN interest_user.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN interest_user.created_at IS '생성 일시';
COMMENT ON COLUMN interest_user.updated_at IS '수정 일시';

-- ========================
-- assets
-- ========================
COMMENT ON TABLE assets IS '업로드 자산(파일/이미지)';
COMMENT ON COLUMN assets.id IS '자동 증가 기본키';
COMMENT ON COLUMN assets.uid IS '자산 UUID(고유)';
COMMENT ON COLUMN assets.origin_file_name IS '원본 파일명';
COMMENT ON COLUMN assets.store_file_name IS '저장 파일명';
COMMENT ON COLUMN assets.file_path IS '저장 경로';
COMMENT ON COLUMN assets.type IS 'MIME 타입 등 파일 유형';
COMMENT ON COLUMN assets.size IS '파일 크기(bytes)';
COMMENT ON COLUMN assets.width IS '이미지 가로(px)';
COMMENT ON COLUMN assets.height IS '이미지 세로(px)';
COMMENT ON COLUMN assets.ext IS '확장자';
COMMENT ON COLUMN assets.download_count IS '다운로드 카운트';
COMMENT ON COLUMN assets.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN assets.created_at IS '생성 일시';
COMMENT ON COLUMN assets.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN assets.updated_at IS '수정 일시';
COMMENT ON COLUMN assets.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN assets.deleted_at IS '삭제 일시';

-- ========================
-- tiers
-- ========================
COMMENT ON TABLE tiers IS '사용자 티어';
COMMENT ON COLUMN tiers.id IS '자동 증가 기본키';
COMMENT ON COLUMN tiers.tier_code IS '티어 코드(고유)';
COMMENT ON COLUMN tiers.division_no IS '구분 번호';
COMMENT ON COLUMN tiers.rank_order IS '랭킹 정렬 순서';
COMMENT ON COLUMN tiers.status IS '상태';
COMMENT ON COLUMN tiers.max_points IS '최대 포인트';
COMMENT ON COLUMN tiers.min_points IS '최소 포인트';
COMMENT ON COLUMN tiers.uid IS 'UUID';
COMMENT ON COLUMN tiers.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN tiers.created_at IS '생성 일시';
COMMENT ON COLUMN tiers.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN tiers.updated_at IS '수정 일시';
COMMENT ON COLUMN tiers.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN tiers.deleted_at IS '삭제 일시';

-- ========================
-- schools
-- ========================
COMMENT ON TABLE schools IS '학교 정보';
COMMENT ON COLUMN schools.id IS '자동 증가 기본키';
COMMENT ON COLUMN schools.sido IS '시/도 코드';
COMMENT ON COLUMN schools.sido_name IS '시/도 명칭';
COMMENT ON COLUMN schools.code IS '학교 코드';
COMMENT ON COLUMN schools.name IS '학교명';
COMMENT ON COLUMN schools.type IS '학교 유형 코드';
COMMENT ON COLUMN schools.type_name IS '학교 유형 명칭';
COMMENT ON COLUMN schools.zipcode IS '우편번호';
COMMENT ON COLUMN schools.address IS '주소';
COMMENT ON COLUMN schools.latitude IS '위도';
COMMENT ON COLUMN schools.longitude IS '경도';
COMMENT ON COLUMN schools.coords IS '좌표/Geo 문자열';
COMMENT ON COLUMN schools.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN schools.created_at IS '생성 일시';
COMMENT ON COLUMN schools.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN schools.updated_at IS '수정 일시';
COMMENT ON COLUMN schools.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN schools.deleted_at IS '삭제 일시';

-- ========================
-- classes
-- ========================
COMMENT ON TABLE classes IS '학급 정보';
COMMENT ON COLUMN classes.id IS '자동 증가 기본키';
COMMENT ON COLUMN classes.code IS '학급 코드';
COMMENT ON COLUMN classes.year IS '년도';
COMMENT ON COLUMN classes.school_id IS '학교 ID(FK)';
COMMENT ON COLUMN classes.school_name IS '학교명(중복저장)';
COMMENT ON COLUMN classes.school_type IS '학교 유형';
COMMENT ON COLUMN classes.class_name IS '반 이름';
COMMENT ON COLUMN classes.major IS '전공';
COMMENT ON COLUMN classes.grade IS '학년';
COMMENT ON COLUMN classes.class IS '반';
COMMENT ON COLUMN classes.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN classes.created_at IS '생성 일시';
COMMENT ON COLUMN classes.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN classes.updated_at IS '수정 일시';
COMMENT ON COLUMN classes.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN classes.deleted_at IS '삭제 일시';

-- ========================
-- user_classes
-- ========================
COMMENT ON TABLE user_classes IS '회원 학급 정보(매핑)';
COMMENT ON COLUMN user_classes.id IS '자동 증가 기본키';
COMMENT ON COLUMN user_classes.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN user_classes.year IS '년도';
COMMENT ON COLUMN user_classes.school_id IS '학교 ID(FK)';
COMMENT ON COLUMN user_classes.class_id IS '학급 ID(FK)';
COMMENT ON COLUMN user_classes.school_type IS '학교 유형';
COMMENT ON COLUMN user_classes.school_name IS '학교명(중복저장)';
COMMENT ON COLUMN user_classes.class_name IS '반 이름(중복저장)';
COMMENT ON COLUMN user_classes.grade IS '학년';
COMMENT ON COLUMN user_classes.auth_school IS '학교 인증 여부';
COMMENT ON COLUMN user_classes.auth_class IS '학급 인증 여부';
COMMENT ON COLUMN user_classes.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN user_classes.created_at IS '생성 일시';
COMMENT ON COLUMN user_classes.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN user_classes.updated_at IS '수정 일시';
COMMENT ON COLUMN user_classes.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN user_classes.deleted_at IS '삭제 일시';

-- ========================
-- pin
-- ========================
COMMENT ON TABLE pin IS '사용자가 등록한 위치 핀';
COMMENT ON COLUMN pin.id IS '자동 증가 기본키';
COMMENT ON COLUMN pin.user_id IS '핀 소유 사용자 ID(FK)';
COMMENT ON COLUMN pin.zipcode IS '우편번호';
COMMENT ON COLUMN pin.lat IS '위도';
COMMENT ON COLUMN pin.lng IS '경도';
COMMENT ON COLUMN pin.description IS '핀 설명';
COMMENT ON COLUMN pin.type IS '핀 유형 코드';
COMMENT ON COLUMN pin.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN pin.created_at IS '생성 일시';
COMMENT ON COLUMN pin.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN pin.updated_at IS '수정 일시';
COMMENT ON COLUMN pin.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN pin.deleted_at IS '삭제 일시';

-- ========================
-- pin_users
-- ========================
COMMENT ON TABLE pin_users IS '핀 참여/연결 사용자';
COMMENT ON COLUMN pin_users.id IS '자동 증가 기본키';
COMMENT ON COLUMN pin_users.pin_id IS '핀 ID(FK)';
COMMENT ON COLUMN pin_users.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN pin_users.created_at IS '생성 일시';
COMMENT ON COLUMN pin_users.deleted_at IS '삭제 일시';

-- ========================
-- report
-- ========================
COMMENT ON TABLE report IS '신고 내역';
COMMENT ON COLUMN report.id IS '자동 증가 기본키';
COMMENT ON COLUMN report.code IS '신고 코드';
COMMENT ON COLUMN report.content IS '신고 내용';
COMMENT ON COLUMN report.status IS '상태(처리중/완료 등)';
COMMENT ON COLUMN report.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN report.created_at IS '생성 일시';
COMMENT ON COLUMN report.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN report.updated_at IS '수정 일시';
COMMENT ON COLUMN report.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN report.deleted_at IS '삭제 일시';

-- ========================
-- friends
-- ========================
COMMENT ON TABLE friends IS '친구 관계';
COMMENT ON COLUMN friends.id IS '자동 증가 기본키';
COMMENT ON COLUMN friends.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN friends.friend_id IS '친구 사용자 ID(FK)';
COMMENT ON COLUMN friends.status IS '상태(요청/수락/차단 등)';
COMMENT ON COLUMN friends.status_at IS '상태 변경 시각';
COMMENT ON COLUMN friends.created_at IS '생성 일시';
COMMENT ON COLUMN friends.updated_at IS '수정 일시';
COMMENT ON COLUMN friends.deleted_at IS '삭제 일시';

-- ========================
-- follows
-- ========================
COMMENT ON TABLE follows IS '팔로우 관계';
COMMENT ON COLUMN follows.id IS '자동 증가 기본키';
COMMENT ON COLUMN follows.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN follows.follow_id IS '팔로우 대상 사용자 ID(FK)';
COMMENT ON COLUMN follows.status IS '상태';
COMMENT ON COLUMN follows.status_at IS '상태 변경 시각';
COMMENT ON COLUMN follows.created_at IS '생성 일시';
COMMENT ON COLUMN follows.updated_at IS '수정 일시';
COMMENT ON COLUMN follows.deleted_at IS '삭제 일시';

-- ========================
-- follow_histories
-- ========================
COMMENT ON TABLE follow_histories IS '팔로우/언팔로우 이력';
COMMENT ON COLUMN follow_histories.id IS '자동 증가 기본키';
COMMENT ON COLUMN follow_histories.user_id IS '사용자 ID(FK, nullable)';
COMMENT ON COLUMN follow_histories.field IS '변경 필드/설명';
COMMENT ON COLUMN follow_histories.created_at IS '기록 생성 일시';

-- ========================
-- boards
-- ========================
COMMENT ON TABLE boards IS '게시판 글';
COMMENT ON COLUMN boards.id IS '자동 증가 기본키';
COMMENT ON COLUMN boards.uid IS '글 UUID';
COMMENT ON COLUMN boards.category IS '카테고리';
COMMENT ON COLUMN boards.subject IS '제목';
COMMENT ON COLUMN boards.content IS '내용 본문';
COMMENT ON COLUMN boards.link IS '관련 링크';
COMMENT ON COLUMN boards.ip IS '작성 IP';
COMMENT ON COLUMN boards.hits IS '조회수';
COMMENT ON COLUMN boards.asset_uid IS '대표 이미지 Asset UID(assets.uid)';
COMMENT ON COLUMN boards.start_date IS '게시 시작일';
COMMENT ON COLUMN boards.end_date IS '게시 종료일';
COMMENT ON COLUMN boards.report_count IS '신고 수';
COMMENT ON COLUMN boards.status IS '상태';
COMMENT ON COLUMN boards.address IS '주소';
COMMENT ON COLUMN boards.detail_address IS '상세 주소';
COMMENT ON COLUMN boards.created_id IS '작성자 사용자 ID';
COMMENT ON COLUMN boards.created_at IS '작성 일시';
COMMENT ON COLUMN boards.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN boards.updated_at IS '수정 일시';
COMMENT ON COLUMN boards.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN boards.deleted_at IS '삭제 일시';

-- ========================
-- board_assets
-- ========================
COMMENT ON TABLE board_assets IS '게시글 첨부 자산 매핑';
COMMENT ON COLUMN board_assets.id IS '자동 증가 기본키';
COMMENT ON COLUMN board_assets.uid IS '첨부 Asset UUID';
COMMENT ON COLUMN board_assets.board_id IS '게시글 ID(FK)';

-- ========================
-- board_comments
-- ========================
COMMENT ON TABLE board_comments IS '게시글 댓글';
COMMENT ON COLUMN board_comments.id IS '자동 증가 기본키';
COMMENT ON COLUMN board_comments.board_id IS '게시글 ID(FK)';
COMMENT ON COLUMN board_comments.uid IS '댓글 UUID';
COMMENT ON COLUMN board_comments.parent_id IS '부모 댓글 ID(대댓글, self-FK)';
COMMENT ON COLUMN board_comments.content IS '댓글 본문';
COMMENT ON COLUMN board_comments.reply_count IS '대댓글 수';
COMMENT ON COLUMN board_comments.report_count IS '신고 수';
COMMENT ON COLUMN board_comments.created_id IS '작성자 사용자 ID';
COMMENT ON COLUMN board_comments.created_at IS '작성 일시';
COMMENT ON COLUMN board_comments.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN board_comments.updated_at IS '수정 일시';
COMMENT ON COLUMN board_comments.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN board_comments.deleted_at IS '삭제 일시';

-- ========================
-- board_likes
-- ========================
COMMENT ON TABLE board_likes IS '게시글 좋아요';
COMMENT ON COLUMN board_likes.id IS '자동 증가 기본키';
COMMENT ON COLUMN board_likes.board_id IS '게시글 ID(FK)';
COMMENT ON COLUMN board_likes.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN board_likes.created_at IS '생성 일시';
COMMENT ON COLUMN board_likes.updated_at IS '수정 일시';
COMMENT ON COLUMN board_likes.deleted_at IS '삭제 일시';

-- ========================
-- board_comment_likes
-- ========================
COMMENT ON TABLE board_comment_likes IS '댓글 좋아요';
COMMENT ON COLUMN board_comment_likes.id IS '자동 증가 기본키';
COMMENT ON COLUMN board_comment_likes.comment_id IS '댓글 ID(FK)';
COMMENT ON COLUMN board_comment_likes.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN board_comment_likes.created_at IS '생성 일시';
COMMENT ON COLUMN board_comment_likes.updated_at IS '수정 일시';
COMMENT ON COLUMN board_comment_likes.deleted_at IS '삭제 일시';

-- ========================
-- polls
-- ========================
COMMENT ON TABLE polls IS '투표(질문)';
COMMENT ON COLUMN polls.id IS '자동 증가 기본키';
COMMENT ON COLUMN polls.question IS '질문 내용';
COMMENT ON COLUMN polls.photo IS '표시용 이미지 경로/URL';
COMMENT ON COLUMN polls.vote_count IS '투표 수';
COMMENT ON COLUMN polls.comment_count IS '댓글 수';
COMMENT ON COLUMN polls.report_count IS '신고 수';
COMMENT ON COLUMN polls.status IS '상태';
COMMENT ON COLUMN polls.reply_at IS '응답 허용/마감 시각';
COMMENT ON COLUMN polls.created_id IS '작성자 사용자 ID';
COMMENT ON COLUMN polls.created_at IS '작성 일시';
COMMENT ON COLUMN polls.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN polls.updated_at IS '수정 일시';
COMMENT ON COLUMN polls.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN polls.deleted_at IS '삭제 일시';

-- ========================
-- poll_items
-- ========================
COMMENT ON TABLE poll_items IS '투표 항목';
COMMENT ON COLUMN poll_items.id IS '자동 증가 기본키';
COMMENT ON COLUMN poll_items.poll_id IS '투표 ID(FK)';
COMMENT ON COLUMN poll_items.seq IS '항목 순번';
COMMENT ON COLUMN poll_items.answer IS '항목 텍스트';
COMMENT ON COLUMN poll_items.votes IS '해당 항목 득표 수';

-- ========================
-- poll_users
-- ========================
COMMENT ON TABLE poll_users IS '투표 참여자';
COMMENT ON COLUMN poll_users.id IS '자동 증가 기본키';
COMMENT ON COLUMN poll_users.poll_id IS '투표 ID(FK)';
COMMENT ON COLUMN poll_users.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN poll_users.voted_at IS '투표 시각';

-- ========================
-- poll_comments
-- ========================
COMMENT ON TABLE poll_comments IS '투표 댓글';
COMMENT ON COLUMN poll_comments.id IS '자동 증가 기본키';
COMMENT ON COLUMN poll_comments.poll_id IS '투표 ID(FK)';
COMMENT ON COLUMN poll_comments.uid IS '댓글 UUID';
COMMENT ON COLUMN poll_comments.parent_id IS '부모 댓글 ID(대댓글, self-FK)';
COMMENT ON COLUMN poll_comments.content IS '댓글 본문';
COMMENT ON COLUMN poll_comments.reply_count IS '대댓글 수';
COMMENT ON COLUMN poll_comments.report_count IS '신고 수';
COMMENT ON COLUMN poll_comments.created_id IS '작성자 사용자 ID';
COMMENT ON COLUMN poll_comments.created_at IS '작성 일시';
COMMENT ON COLUMN poll_comments.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN poll_comments.updated_at IS '수정 일시';
COMMENT ON COLUMN poll_comments.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN poll_comments.deleted_at IS '삭제 일시';

-- ========================
-- poll_comment_likes
-- ========================
COMMENT ON TABLE poll_comment_likes IS '투표 댓글 좋아요';
COMMENT ON COLUMN poll_comment_likes.id IS '자동 증가 기본키';
COMMENT ON COLUMN poll_comment_likes.poll_comment_id IS '투표 댓글 ID(FK)';
COMMENT ON COLUMN poll_comment_likes.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN poll_comment_likes.created_at IS '생성 일시';
COMMENT ON COLUMN poll_comment_likes.updated_at IS '수정 일시';
COMMENT ON COLUMN poll_comment_likes.deleted_at IS '삭제 일시';

-- ========================
-- chat_rooms
-- ========================
COMMENT ON TABLE chat_rooms IS '채팅방';
COMMENT ON COLUMN chat_rooms.id IS '자동 증가 기본키';
COMMENT ON COLUMN chat_rooms.uid IS '채팅방 UUID';
COMMENT ON COLUMN chat_rooms.last_user_id IS '마지막 발신자 사용자 ID';
COMMENT ON COLUMN chat_rooms.last_message_id IS '마지막 메시지 ID';
COMMENT ON COLUMN chat_rooms.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN chat_rooms.created_at IS '생성 일시';
COMMENT ON COLUMN chat_rooms.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN chat_rooms.updated_at IS '수정 일시';
COMMENT ON COLUMN chat_rooms.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN chat_rooms.deleted_at IS '삭제 일시';

-- ========================
-- chat_users
-- ========================
COMMENT ON TABLE chat_users IS '채팅방 참여자';
COMMENT ON COLUMN chat_users.id IS '자동 증가 기본키';
COMMENT ON COLUMN chat_users.chat_room_id IS '채팅방 ID(FK)';
COMMENT ON COLUMN chat_users.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN chat_users.status IS '참여 상태(0정상/1뮤트/2차단 등)';
COMMENT ON COLUMN chat_users.push IS '푸시 알림 허용 여부';
COMMENT ON COLUMN chat_users.push_at IS '푸시 설정 변경 시각';
COMMENT ON COLUMN chat_users.joining_at IS '입장 시각';
COMMENT ON COLUMN chat_users.leaving_at IS '퇴장 시각';
COMMENT ON COLUMN chat_users.deleted_at IS '삭제 일시';

-- ========================
-- chat_messages
-- ========================
COMMENT ON TABLE chat_messages IS '채팅 메시지';
COMMENT ON COLUMN chat_messages.id IS '자동 증가 기본키';
COMMENT ON COLUMN chat_messages.chat_room_id IS '채팅방 ID(FK)';
COMMENT ON COLUMN chat_messages.user_id IS '작성 사용자 ID(FK)';
COMMENT ON COLUMN chat_messages.uid IS '메시지 UUID';
COMMENT ON COLUMN chat_messages.content IS '메시지 본문';
COMMENT ON COLUMN chat_messages.read_count IS '읽음 수';
COMMENT ON COLUMN chat_messages.created_at IS '작성 일시';
COMMENT ON COLUMN chat_messages.updated_at IS '수정 일시';
COMMENT ON COLUMN chat_messages.deleted_at IS '삭제 일시';

-- ========================
-- chat_messages_assets
-- ========================
COMMENT ON TABLE chat_messages_assets IS '메시지 첨부 자산';
COMMENT ON COLUMN chat_messages_assets.id IS '자동 증가 기본키';
COMMENT ON COLUMN chat_messages_assets.uid IS '첨부 Asset UUID';
COMMENT ON COLUMN chat_messages_assets.message_id IS '메시지 ID(FK)';
COMMENT ON COLUMN chat_messages_assets.width IS '이미지 가로(px)';
COMMENT ON COLUMN chat_messages_assets.height IS '이미지 세로(px)';

-- ========================
-- point_rules
-- ========================
COMMENT ON TABLE point_rules IS '포인트 규칙';
COMMENT ON COLUMN point_rules.id IS '자동 증가 기본키';
COMMENT ON COLUMN point_rules.action_code IS '행동 코드(고유)';
COMMENT ON COLUMN point_rules.point IS '지급 포인트';
COMMENT ON COLUMN point_rules.daily_cap IS '하루 최대 지급 한도';
COMMENT ON COLUMN point_rules.cooldown_sec IS '쿨다운(초)';
COMMENT ON COLUMN point_rules.created_id IS '생성자 사용자 ID';
COMMENT ON COLUMN point_rules.created_at IS '생성 일시';
COMMENT ON COLUMN point_rules.updated_id IS '수정자 사용자 ID';
COMMENT ON COLUMN point_rules.updated_at IS '수정 일시';
COMMENT ON COLUMN point_rules.deleted_id IS '삭제자 사용자 ID';
COMMENT ON COLUMN point_rules.deleted_at IS '삭제 일시';

-- ========================
-- point_histories
-- ========================
COMMENT ON TABLE point_histories IS '포인트 획득 이력';
COMMENT ON COLUMN point_histories.id IS '자동 증가 기본키';
COMMENT ON COLUMN point_histories.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN point_histories.point IS '지급 포인트';
COMMENT ON COLUMN point_histories.meta_json IS '부가 메타데이터(JSON)';
COMMENT ON COLUMN point_histories.created_at IS '기록 생성 일시';

-- ========================
-- user_photos
-- ========================
COMMENT ON TABLE user_photos IS '사용자 사진(프로필 등)';
COMMENT ON COLUMN user_photos.id IS '자동 증가 기본키';
COMMENT ON COLUMN user_photos.user_id IS '사용자 ID(FK)';
COMMENT ON COLUMN user_photos.uid IS '사진 Asset UUID(매핑용)';

-- ========================
-- location_histories (테이블을 활성화하면 아래 코멘트도 함께 사용하세요)
-- ========================
-- COMMENT ON TABLE location_histories IS '사용자 위치 이력';
-- COMMENT ON COLUMN location_histories.id IS '자동 증가 기본키';
-- COMMENT ON COLUMN location_histories.user_id IS '사용자 ID(FK)';
-- COMMENT ON COLUMN location_histories.lat IS '위도';
-- COMMENT ON COLUMN location_histories.lng IS '경도';
-- COMMENT ON COLUMN location_histories."timestamp" IS '위치 기록 시각';
-- COMMENT ON COLUMN location_histories.created_at IS '생성 일시';

-- ========================
-- mqtt_credentials (테이블을 활성화하면 아래 코멘트도 함께 사용하세요)
-- ========================
-- COMMENT ON TABLE mqtt_credentials IS 'MQTT 인증 자격 정보';
-- COMMENT ON COLUMN mqtt_credentials.id IS '자동 증가 기본키';
-- COMMENT ON COLUMN mqtt_credentials.user_id IS '사용자 ID(FK)';
-- COMMENT ON COLUMN mqtt_credentials.credentials_id IS '자격 ID(외부 시스템 키)';
-- COMMENT ON COLUMN mqtt_credentials.client_id IS 'MQTT 클라이언트 ID';
-- COMMENT ON COLUMN mqtt_credentials.username IS 'MQTT 사용자명';
-- COMMENT ON COLUMN mqtt_credentials.password IS 'MQTT 비밀번호/해시';
-- COMMENT ON COLUMN mqtt_credentials.issued_at IS '발급 시각';
-- COMMENT ON COLUMN mqtt_credentials.expires_at IS '만료 시각';


