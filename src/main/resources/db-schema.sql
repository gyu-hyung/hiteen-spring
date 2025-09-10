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
  phone         varchar(30),
  mood          varchar(30),
  tier          varchar(30),
  asset_uid     uuid, -- REFERENCES assets(uid),
  school_id     bigint,
  grade         varchar(30),
  gender        varchar(30),
  birthday		date,
  created_id    bigint,
  created_at    timestamptz not null DEFAULT now(),
  updated_id    bigint,
  updated_at    timestamptz,
  deleted_id    bigint,
  deleted_at    timestamptz
);

-- 이메일: 삭제되지 않은 사용자만 유니크, 대소문자 구분 없음
--CREATE UNIQUE INDEX users_email_key
--    ON users (lower(email))
--    WHERE deleted_at IS NULL;

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
  found_date  date,
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
  school_id    bigint,
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
  school_id    bigint  ,
  class_id     bigint  ,
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
-- SMS 전송내역
-- ========================
create table sms (
    id bigserial primary key,
    title varchar(255),
    content varchar(255),
    callback varchar(30),
    total bigint,
    success bigint,
    failure bigint,
    created_id bigint,
    created_at timestamptz not null default now(),
    updated_at timestamptz
);

-- ========================
-- SMS 전송상세
-- ========================
create table sms_details (
    id bigserial primary key,
    sms_id bigint not null,
    phone varchar(500) not null,
    success smallint not null default 0,
    error varchar(300),
    created_at timestamptz not null default now(),
    updated_at timestamptz,
    deleted_at timestamptz
);

-- ========================
-- SMS 인증내역
-- ========================
create table sms_auth (
    id bigserial PRIMARY KEY,
    sms_id bigint not null,
    phone varchar(30) not null,
    code varchar(255) not null,
    status varchar(255) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz,
    deleted_at timestamptz
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
  visibility   varchar(20) DEFAULT 'PUBLIC',
  created_id   bigint,
  created_at   timestamptz DEFAULT now(),
  updated_id   bigint ,
  updated_at   timestamptz,
  deleted_id   bigint ,
  deleted_at   timestamptz
);


-- ========================
-- 위치 핀 > 사용자 공개
-- ========================
CREATE TABLE pin_users (
  id         bigserial PRIMARY KEY,
  pin_id     bigint   NOT NULL,
  user_id    bigint   NOT NULL,
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
  id          			bigserial PRIMARY KEY,
  chat_room_id 			bigint NOT NULL REFERENCES chat_rooms(id) ON DELETE CASCADE,
  user_id     			bigint NOT NULL REFERENCES users(id)       ON DELETE CASCADE,
  last_read_message_id 	bigint,
  last_read_at 			timestamptz,
  status      			smallint,    -- 예: 0=정상, 1=뮤트, 2=차단...
  push        			boolean,
  push_at     			timestamptz,
  joining_at  			timestamptz,
  leaving_at  			timestamptz,
  deleted_at  			timestamptz,
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
  kind         smallint not null default 0,
  emoji_code   varchar(64),
--  read_count   integer DEFAULT 0,
  created_at   timestamptz DEFAULT now(),
  updated_at   timestamptz,
  deleted_at   timestamptz
);


-- 히스토리(이모지 제외) 조회 최적화 인덱스
--CREATE INDEX IF NOT EXISTS chat_msg_room_created_hist_idx
--  ON chat_messages (chat_room_id, created_at DESC)
--  WHERE deleted_at IS NULL AND kind <> 2;

-- 필요 시 일반 전체 인덱스도 유지
--CREATE INDEX IF NOT EXISTS chat_msg_room_created_all_idx
--  ON chat_messages (chat_room_id, created_at DESC)
--  WHERE deleted_at IS NULL;


-- 이제 chat_rooms.last_message_id FK 연결(순환 참조 방지 위해 나중에 추가)
ALTER TABLE chat_rooms
  ADD CONSTRAINT chat_rooms_last_msg_fk
  FOREIGN KEY (last_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL;

-- ========================
-- 채팅 > 메세지 > 파일
-- ========================
CREATE TABLE chat_messages_assets (
  id         bigserial PRIMARY KEY,
  uid        uuid NOT NULL,
--  uid        uuid NOT NULL DEFAULT gen_random_uuid(),
  message_id bigint NOT NULL,
  width      integer,
  height     integer
);

-- 활성 멤버 조회/갱신 최적화
CREATE INDEX IF NOT EXISTS idx_chat_users_room_user_active ON chat_users (chat_room_id, user_id) WHERE deleted_at IS NULL;



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
--  users,
--  sms,
--  sms_details,
--  sms_auth
--CASCADE;
