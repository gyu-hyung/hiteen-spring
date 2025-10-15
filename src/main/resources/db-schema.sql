-- ========================
-- hiteen DDL
-- ========================

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;   -- case-insensitive text
CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;



-- ========================
-- 티어
-- ========================
CREATE TABLE tiers (
  id           bigserial PRIMARY KEY,
  tier_code    varchar(30) NOT NULL, -- "BRONZE", "SILVER", "GOLD"
  tier_name_kr varchar(50) NOT NULL,
  division_no  smallint,             -- 1,2,3 단계 (예: 브론즈 1,2,3)
  rank_order   integer,              -- 전체 순서 (1=브론즈1, 2=브론즈2…)
  status       varchar(20) DEFAULT 'ACTIVE',
  min_points   integer NOT NULL,     -- 해당 티어 최소 포인트
  max_points   integer NOT NULL,     -- 해당 티어 최대 포인트
  uid          uuid DEFAULT gen_random_uuid(),
  created_at   timestamptz DEFAULT now(),
  updated_at   timestamptz,
  deleted_at   timestamptz,
  UNIQUE (tier_code, division_no)
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
-- 사용자
-- ========================
CREATE TABLE users (
  id                bigserial PRIMARY KEY,
  uid               uuid NOT NULL DEFAULT gen_random_uuid(),
  username          varchar(50),
  email             varchar(255),
  nickname          varchar(50),
  password          varchar(255),
  role              varchar(30),
  address           varchar(255),
  detail_address    varchar(255),
  phone             varchar(30),
  mood              varchar(30),
  mood_emoji        varchar(30),
  mbti              varchar(30),
  exp_points        bigint DEFAULT 0,
  tier_id           bigint REFERENCES tiers(id),
  asset_uid         uuid REFERENCES assets(uid),
  school_id         bigint,
  grade             varchar(30),
  gender            varchar(30),
  birthday		    date,
  profile_decoration_code VARCHAR(50),
  invite_code       varchar(30),
  invite_joins      bigint,
  created_id        bigint,
  created_at        timestamptz not null DEFAULT now(),
  updated_id        bigint,
  updated_at        timestamptz,
  deleted_id        bigint,
  deleted_at        timestamptz
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
-- 사용자 상세정보
-- ========================
CREATE TABLE user_details (
  id            bigserial PRIMARY KEY,
  user_id       bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_id     VARCHAR(300),
  device_os     VARCHAR(30),
  device_version VARCHAR(20),
  device_detail text,
  device_token  VARCHAR(300),
  location_token VARCHAR(300),
  aqns_token    VARCHAR(300),
  api_token     VARCHAR(300),
  agree_service VARCHAR(100),
  agree_privacy VARCHAR(100),
  agree_finance VARCHAR(100),
  agree_marketing VARCHAR(100),
  push_service  VARCHAR(100),
  push_marketing VARCHAR(100),
  push_items    text,
  memo          text
);


-- ========================
-- 사용 사진
-- ========================
CREATE TABLE user_photos (
  id      bigserial PRIMARY KEY,
  user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  uid     uuid NOT NULL REFERENCES assets(uid),
  UNIQUE (uid),
  UNIQUE (user_id, uid)
);


-- ========================
-- 사용자 연락처
-- ========================
CREATE TABLE user_contacts (
    id          bigserial PRIMARY KEY,
    user_id     bigint NOT NULL,
    phone       varchar(20) NOT NULL,
    name        varchar(50),
    created_at  timestamptz DEFAULT now(),
    updated_at  timestamptz DEFAULT now(),
    UNIQUE (user_id, phone)
);


-- ========================
-- 코드
-- ========================
CREATE TABLE codes (
  id              bigserial PRIMARY KEY,
  code            varchar(50)  NOT NULL,
  code_name       varchar(100) NOT NULL,
  code_group      varchar(50),
  code_group_name varchar(100), -- 관심사 등
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
-- 코드 > 파일 (자산)
-- ========================
CREATE TABLE code_assets (
  id          bigserial PRIMARY KEY,
  code_id     bigint NOT NULL REFERENCES codes(id) ON DELETE CASCADE,
  uid         uuid   NOT NULL REFERENCES assets(uid) ON DELETE CASCADE,
  created_id  bigint,
  created_at  timestamptz DEFAULT now()
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
-- 사용자-관심사
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
-- 사용자 관심사 추천/매칭 이력
-- ========================
CREATE TABLE interest_match_history (
  id          bigserial PRIMARY KEY,
  user_id     bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target_id   bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status      varchar(20) NOT NULL,  -- PASSED, MATCHED, etc
  created_at  timestamptz DEFAULT now(),
  UNIQUE (user_id, target_id)
);


-- ========================
-- 획득 경험치 이력
-- ========================
CREATE TABLE user_exp_history (
  id          bigserial PRIMARY KEY,
  user_id     bigint NOT NULL REFERENCES users(id),
  target_id   bigint,
  action_code varchar(50) NOT NULL,   -- 활동 코드 (POST, COMMENT, LOGIN, FRIEND_ADD 등)
  points      integer NOT NULL,       -- 획득/차감된 점수 (+10, -5 등)
  reason      varchar(255),           -- 상세 설명 (ex: "게시글 작성 보상")
  created_at  timestamptz DEFAULT now()
);


-- ========================
-- 포인트 적립/사용 내역
-- ========================
CREATE TABLE points (
  id             BIGSERIAL PRIMARY KEY,
  user_id        BIGINT NOT NULL REFERENCES users(id),
  pointable_type VARCHAR(100),   -- 어떤 이벤트인지 (AD, GAME, PAYMENT 등)
  pointable_id   BIGINT,         -- 연관 데이터 (게임ID, 광고 트랜잭션ID 등)
  type           VARCHAR(50) NOT NULL, -- 'CREDIT' or 'DEBIT'
  point          INT NOT NULL,
  memo           VARCHAR(300),
  created_at     TIMESTAMPTZ DEFAULT now(),
  deleted_at     TIMESTAMPTZ
);

-- 조회 자주할 필드 인덱스
CREATE INDEX idx_points_user_id ON points(user_id);
CREATE INDEX idx_points_pointable ON points(pointable_type, pointable_id);


-- ========================
-- 포인트 요약정보
-- ========================
CREATE TABLE user_points_summary (
  user_id BIGINT PRIMARY KEY REFERENCES users(id),
  total_point INT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ DEFAULT now()
);


-- ========================
-- 포인트
-- ========================
CREATE TABLE point_rules (
  id           bigserial PRIMARY KEY,
  action_code  varchar(50)  NOT NULL,
  point        integer NOT NULL,
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
-- 학교
-- ========================
CREATE TABLE schools (
  id          bigserial PRIMARY KEY,
  sido        varchar(20),
  sido_name   varchar(50),
  code        varchar(30),
  name        varchar(100),
  type        integer,
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
-- 학교 > 급식
-- ========================
CREATE TABLE school_food (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    meal_date DATE NOT NULL,
    code VARCHAR(10) NOT NULL,
    code_name VARCHAR(50),
    meals TEXT,
    calorie VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    deleted_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (school_id, meal_date, code)
);


-- ========================
-- 학교 > 급식 > 이미지
-- ========================
CREATE TABLE school_food_image (
    id BIGSERIAL PRIMARY KEY,
    school_id BIGINT NOT NULL,
    year SMALLINT NOT NULL DEFAULT 0,
    month SMALLINT NOT NULL DEFAULT 0,
    user_id BIGINT,
    image VARCHAR(100),              -- 파일 UID
    report_count INT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1, -- 1: 노출, 0: 미노출
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);



-- ========================
-- 학급
-- ========================
CREATE TABLE school_classes (
  id           bigserial PRIMARY KEY,
  code         varchar(50),
  year         smallint,
  school_id    bigint,
  school_name  varchar(100),
  school_type  varchar(20),
  class_name   varchar(50),
  major        varchar(50),
  grade        varchar(10),
  classNo      varchar(100),
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
-- 학급 시간표
-- ========================
CREATE TABLE time_table (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    year SMALLINT NOT NULL DEFAULT 0,
    semester SMALLINT NOT NULL DEFAULT 0,
    time_date DATE NOT NULL,
    period SMALLINT NOT NULL DEFAULT 0,
    subject VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_time_table_class_date_period UNIQUE (class_id, time_date, period)
);

CREATE INDEX idx_time_table_class_date
    ON time_table (class_id, time_date);


-- ========================
-- 사용자 등록 시간표
-- ========================
CREATE TABLE time_user (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    user_id BIGINT,
    week SMALLINT NOT NULL DEFAULT 0,
    period SMALLINT NOT NULL DEFAULT 0,
    subject VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);


-- ========================
-- 시간표 이미지
-- ========================
CREATE TABLE time_image (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    semester SMALLINT NOT NULL DEFAULT 1,
    user_id BIGINT,
    image VARCHAR(100),
    report_count INT NOT NULL DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);


-- ========================
-- 초대 내역
-- ========================
CREATE TABLE invites (
    id          BIGSERIAL PRIMARY KEY,                -- 고유번호
    type        VARCHAR(30)    NOT NULL DEFAULT 'Register', -- 초대구분 (Invite, Referral, Register)
    user_id     BIGINT,                               -- 회원번호(초대한 회원)
    phone       VARCHAR(200)  NOT NULL,               -- 초대한 연락처
    code        VARCHAR(100),                         -- 초대코드
    status      smallint       NOT NULL DEFAULT 0,     -- 상태 (가입, 탈퇴 등)
    join_id     BIGINT,                               -- 가입 회원번호
    join_point  INT           NOT NULL DEFAULT 0,     -- 초대가입 지급포인트
    join_date   timestamptz,                            -- 가입일시
    leave_date  timestamptz                             -- 탈퇴일시
);


-- ========================
-- 출석체크
-- ========================
CREATE TABLE attends (
    id         BIGSERIAL PRIMARY KEY,         -- 고유번호
    user_id    BIGINT NOT NULL,               -- 회원번호
    attend_date DATE NOT NULL,                -- 출석일
    sum_day    SMALLINT NOT NULL DEFAULT 0,   -- 누적 출석일수
    point      INT NOT NULL DEFAULT 0,        -- 총 지급 포인트
    add_point  INT NOT NULL DEFAULT 0,        -- 추가 포인트
    created_at TIMESTAMPTZ DEFAULT now()      -- 체크일시
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
CREATE TABLE reports (
    id              BIGSERIAL PRIMARY KEY, -- AUTO_INCREMENT 대신 BIGSERIAL
    user_id         BIGINT       NOT NULL, -- 신고자 회원번호
    target_id       BIGINT       NULL,     -- 신고대상 회원번호
    type            VARCHAR(30)  NOT NULL, -- 신고 구분
    reportable_type VARCHAR(100) NULL,     -- 컨텐츠 모델
    reportable_id   BIGINT       NULL,     -- 컨텐츠 번호
    reason          VARCHAR(255) NULL,     -- 신고사유
    photo_uid       VARCHAR(100) NULL,     -- 신고사진 UID
    status          SMALLINT     NOT NULL DEFAULT 0, -- 대기(0), 완료(1)
    answer          TEXT         NULL,     -- 답변내용
    answer_at       TIMESTAMPTZ  NULL,     -- 답변일시
    memo            TEXT         NULL,     -- 조치내용
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(), -- 신고일시
    updated_at      TIMESTAMPTZ  NULL,     -- 변경일시
    deleted_at      TIMESTAMPTZ  NULL      -- 삭제일시
);


-- ========================
-- 친구
-- ========================
CREATE TABLE friends (
  id                    bigserial PRIMARY KEY,
  user_id               bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  friend_id             bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  status                varchar(20),
  status_at             timestamptz,
  user_location_mode    varchar(20) DEFAULT 'PUBLIC',
  friend_location_mode  varchar(20) DEFAULT 'PUBLIC',
  created_at            timestamptz DEFAULT now(),
  updated_at            timestamptz,
  deleted_at            timestamptz,
  CONSTRAINT friends_not_self CHECK (user_id <> friend_id),
  UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_friends_user ON friends(user_id);
CREATE INDEX idx_friends_friend ON friends(friend_id);
CREATE INDEX idx_friends_status_user ON friends(status, user_id);
CREATE INDEX idx_friends_status_friend ON friends(status, friend_id);

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
  ip             varchar(100),
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
  uid       uuid NOT NULL REFERENCES assets(uid),
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


CREATE UNIQUE INDEX IF NOT EXISTS ux_boards_uid ON boards(uid);
CREATE INDEX IF NOT EXISTS ix_boards_created_at ON boards(created_at DESC);
CREATE INDEX IF NOT EXISTS ix_boards_category ON boards(category);
CREATE INDEX IF NOT EXISTS ix_bc_board_id ON board_comments(board_id);
CREATE INDEX IF NOT EXISTS ix_bc_parent_id ON board_comments(parent_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_board_likes ON board_likes(board_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_comment_likes ON board_comment_likes(comment_id, user_id);


-- ========================
-- 투표
-- ========================
CREATE TABLE polls (
  id             bigserial PRIMARY KEY,
  question       varchar(255),
  photo          uuid REFERENCES assets(uid),
  selects        jsonb,
  color_start    varchar(20),
  color_end      varchar(20),
  vote_count	 smallint DEFAULT 0,
  comment_count  smallint DEFAULT 0,
  report_count   smallint DEFAULT 0,
  allow_comment  smallint DEFAULT 0,
  status         varchar(20),
  created_id     bigint ,
  created_at     timestamptz DEFAULT now(),
  updated_at     timestamptz,
  deleted_at     timestamptz
);


-- ========================
-- 투표 > 사진
-- ========================
CREATE TABLE poll_photos (
  id          bigserial PRIMARY KEY,
  poll_id     bigint NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
  uid         uuid   NOT NULL REFERENCES assets(uid) ON DELETE CASCADE,
  seq         smallint NOT NULL,
  created_at  timestamptz DEFAULT now()
);


-- ========================
-- 투표 > 좋아요
-- ========================
CREATE TABLE poll_likes (
  id         bigserial PRIMARY KEY,
  poll_id    bigint NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
  user_id    bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  created_at timestamptz DEFAULT now(),
  UNIQUE (poll_id, user_id)
);


-- ========================
-- 투표 > 회원
-- ========================
CREATE TABLE poll_users (
  id        bigserial PRIMARY KEY,
  poll_id   bigint NOT NULL REFERENCES polls(id)  ON DELETE CASCADE,
  user_id   bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  seq       smallint NOT NULL,
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
  updated_at   timestamptz,
  deleted_at   timestamptz
);


-- ========================
-- 투표 > 댓글 > 좋아요
-- ========================
CREATE TABLE poll_comment_likes (
  id              	bigserial PRIMARY KEY,
  comment_id 		bigint NOT NULL REFERENCES poll_comments(id) ON DELETE CASCADE,
  user_id    	    bigint NOT NULL REFERENCES users(id)         ON DELETE CASCADE,
  created_at      	timestamptz DEFAULT now(),
--  updated_at      timestamptz,
--  deleted_at      timestamptz,
  UNIQUE (comment_id, user_id)
);


-- ========================
-- 투표 > 템플릿
-- ========================
CREATE TABLE poll_templates (
  id            bigserial PRIMARY KEY,
  question      text,
  answers       jsonb,
  state         smallint,
  created_at    timestamptz DEFAULT now(),
  updated_at    timestamptz,
  deleted_at    timestamptz
);


-- ========================
-- 게임 회차
-- ========================
CREATE TABLE seasons (
  id          bigserial PRIMARY KEY,
  season_no   integer NOT NULL, -- 1,2,3... (자동 계산용)
  start_date  date NOT NULL,
  end_date    date NOT NULL,
  status      varchar(20) DEFAULT 'ACTIVE', -- ACTIVE, CLOSED
  created_at  timestamptz DEFAULT now(),
  updated_at  timestamptz
);


-- ========================
-- 게임 회차 > 참가자
-- ========================
CREATE TABLE season_participants (
  id bigserial PRIMARY KEY,
  season_id bigint NOT NULL REFERENCES seasons(id),
  user_id bigint NOT NULL REFERENCES users(id),
  tier_id bigint NOT NULL,  -- 시즌 시작 시점의 고정 티어
  league varchar(20) NOT NULL,
  joined_at timestamptz DEFAULT now(),
  joined_type varchar(20) DEFAULT 'INITIAL',-- INITIAL(시즌시작), BRONZE_JOIN(중간참여)
  UNIQUE(season_id, user_id)
);



-- ========================
-- 게임 종류
-- ========================
CREATE TABLE games (
  id          bigserial PRIMARY KEY,
  code        varchar(50) NOT NULL UNIQUE, -- ex) NUMBER_SPEED, MEMORY, ENGLISH, BINGO
  name        varchar(100) NOT NULL,       -- 노출 이름
  description text,
  status      varchar(20) DEFAULT 'ACTIVE',
  created_at  timestamptz DEFAULT now(),
  updated_at  timestamptz,
  deleted_at  timestamptz
);


-- ========================
-- 게임 점수 이력
-- ========================
CREATE TABLE game_scores (
  id            bigserial PRIMARY KEY,
  season_id 	bigint NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
  participant_id bigint NOT NULL REFERENCES season_participants(id) ON DELETE CASCADE,
  game_id       bigint NOT NULL REFERENCES games(id),
  score         DOUBLE PRECISION NOT NULL,
  try_count     integer DEFAULT 1,
  created_at    timestamptz DEFAULT now(),
  updated_at    timestamptz,
  deleted_at    timestamptz,
  UNIQUE (participant_id, game_id) -- 시즌/유저 단위 고유성 보장
);


-- ========================
-- 게임 랭킹
-- ========================
CREATE TABLE game_rankings (
  id             BIGSERIAL PRIMARY KEY,
  season_id      BIGINT NOT NULL REFERENCES seasons(id) ON DELETE CASCADE,
  league 		 varchar(20),
  game_id        BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
  rank           INTEGER NOT NULL,
  score          INTEGER NOT NULL,
  participant_id BIGINT NOT NULL REFERENCES season_participants(id) ON DELETE CASCADE,
  user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  nickname       VARCHAR(50) NOT NULL,
  profile_image  VARCHAR(255),
  created_at     TIMESTAMPTZ DEFAULT now()
);


-- ========================
-- 영단어 학습
-- ========================
CREATE TABLE question (
	id bigserial PRIMARY KEY,
	"type" bigint NOT NULL,
	category varchar(255) NULL,
	question varchar(255) NULL,
	symbol varchar(255) NULL,
	sound varchar(255) NULL,
	answer varchar(255) NULL,
	"content" text NULL,
	status bigint NOT NULL,
	created_at timestamptz NULL,
	updated_at timestamptz NULL,
	deleted_at timestamptz NULL
);


-- ========================
-- 영단어 학습
-- ========================
CREATE TABLE question_items (
	id bigserial PRIMARY KEY,
	season_id bigint NOT NULL,
	"type" bigint NULL,
	question_id bigint NULL,
	answers text NULL
);


-- ========================
-- 영단어 학습
-- ========================
CREATE TABLE study (
	id bigserial PRIMARY KEY,
	uid varchar(100) NULL,
	user_id bigint NOT NULL,
	season_id bigint NOT NULL,
	study_items text NULL,
	give_point bigint NOT NULL,
	status bigint NOT NULL,
	complete_date timestamptz NULL,
	prep bigint NOT NULL,
	prep_point bigint NOT NULL,
	prep_date timestamptz NULL,
	created_at timestamptz NULL,
	updated_at timestamptz NULL,
	deleted_at timestamptz NULL
);


-- ========================
-- 상품 지급 내역
-- ========================
CREATE TABLE rewards (
  id          bigserial PRIMARY KEY,
  season_id   bigint NOT NULL REFERENCES seasons(id),
  user_id     bigint NOT NULL REFERENCES users(id),
  league_code varchar(30) NOT NULL, -- BRONZE / PLATINUM / CHALLENGER
  reward_type varchar(50),          -- GIFT_CARD, COUPON...
  reward_value varchar(100),        -- "올영상품권 10,000원"
  status      varchar(20) DEFAULT 'PENDING', -- PENDING / SENT / CANCELLED
  created_at  timestamptz DEFAULT now(),
  updated_at  timestamptz
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
CREATE INDEX IF NOT EXISTS chat_msg_room_created_hist_idx
  ON chat_messages (chat_room_id, created_at DESC)
  WHERE deleted_at IS NULL AND kind <> 2;

-- 필요 시 일반 전체 인덱스도 유지
CREATE INDEX IF NOT EXISTS chat_msg_room_created_all_idx
  ON chat_messages (chat_room_id, created_at DESC)
  WHERE deleted_at IS NULL;


-- 이제 chat_rooms.last_message_id FK 연결(순환 참조 방지 위해 나중에 추가)
ALTER TABLE chat_rooms
  ADD CONSTRAINT chat_rooms_last_msg_fk
  FOREIGN KEY (last_message_id) REFERENCES chat_messages(id) ON DELETE SET NULL;

-- ========================
-- 채팅 > 메세지 > 파일
-- ========================
CREATE TABLE chat_messages_assets (
  id         bigserial PRIMARY KEY,
  uid        uuid NOT NULL REFERENCES assets(uid),
  message_id bigint NOT NULL REFERENCES chat_messages(id),
  width      integer,
  height     integer
);

-- 활성 멤버 조회/갱신 최적화
CREATE INDEX IF NOT EXISTS idx_chat_users_room_user_active ON chat_users (chat_room_id, user_id) WHERE deleted_at IS NULL;



-- ========================
-- 푸시 전송내역
-- ========================
CREATE TABLE push (
    id BIGSERIAL PRIMARY KEY,                  -- 전송번호
    type VARCHAR(50),                          -- 전송구분 (toast, dialog, notification)
    code VARCHAR(50),                          -- 구분코드
    title VARCHAR(255),                        -- 제목
    message TEXT,                              -- 내용
    total BIGINT NOT NULL DEFAULT 0,           -- 전송건수
    success BIGINT NOT NULL DEFAULT 0,         -- 성공건수
    failure BIGINT NOT NULL DEFAULT 0,         -- 실패건수
    multicast_id VARCHAR(255),                 -- 다중ID
    canonical_ids VARCHAR(255),                -- 발송ID
    created_id BIGINT,                         -- 전송 회원번호
    created_at timestamptz,                      -- 전송일시
    updated_at timestamptz,                      -- 수정일시
    deleted_at timestamptz                       -- 삭제일시
);



-- ========================
-- 푸시 전송상세
-- ========================
CREATE TABLE push_detail (
    id BIGSERIAL PRIMARY KEY,                  -- 상세번호
    push_id BIGINT NOT NULL REFERENCES push(id), -- 전송번호 (FK)
    user_id BIGINT,                            -- 수신자 회원번호
    device_os VARCHAR(50),                     -- 디바이스 OS
    device_token VARCHAR(300),                 -- 디바이스 토큰
    phone VARCHAR(20),                         -- 전화번호
    multicast_id VARCHAR(300),                 -- 다중ID
    message_id VARCHAR(300),                   -- 메시지ID
    registration_id VARCHAR(300),              -- 인증ID
    error TEXT,                                -- 에러내용
    success SMALLINT NOT NULL DEFAULT 0,       -- 성공여부 (0/1)
    created_at timestamptz,                      -- 전송일시
    updated_at timestamptz,                      -- 수정일시
    deleted_at timestamptz                       -- 삭제일시
);



-- ========================
-- 광고 리워드 검증용 로그
-- ========================
CREATE TABLE admob_rewards (
  id             BIGSERIAL PRIMARY KEY,
  transaction_id VARCHAR(100) UNIQUE NOT NULL,
  user_id        BIGINT NOT NULL REFERENCES users(id),
  reward         INT NOT NULL,
  raw_data       JSONB, -- webhook 전체 데이터 저장
  created_at     timestamptz DEFAULT now()
);


-- ========================
-- 스크린타임
-- ========================
CREATE TABLE user_session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_time timestamptz NOT NULL,
    end_time timestamptz,
    duration_minutes INT,
    status VARCHAR(20) NOT NULL
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


DROP TABLE IF EXISTS
  -- 관계(자식) 테이블들 먼저
  chat_messages_assets,
  chat_messages,
  chat_users,
  chat_rooms,
  poll_comment_likes,
  poll_comments,
  poll_likes,
  poll_users,
  poll_photos,
  polls,
  poll_templates,
  board_comment_likes,
  board_comments,
  board_likes,
  board_assets,
  boards,
  friends,
  follows,
  follow_histories,
  pin_users,
  pin,
  reports,
  code_assets,
  interest_match_history,
  interest_user,
  interests,
  user_exp_history,
  point_histories,
  point_rules,
  points,
  user_points_summary,
  rewards,
  game_rankings,
  game_scores,
  season_participants,
  seasons,
  games,
  question_items,
  question,
  study,
  user_classes,
  school_food_image,
  school_food,
  school_classes,
  schools,
  time_image,
  time_table,
  time_user,
  user_contacts,
  user_details,
  user_photos,
  invites,
  attends,
  sms_auth,
  sms_details,
  sms,
  push_detail,
  push,
  admob_rewards,
  assets,
  tiers,
  user_session,
  codes,
  users;



INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(1, '급식에 평생 추가된다면?', '["치킨", "피자"]'::jsonb, 1, '2025-09-26 13:44:15.203', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(2, '시험 전날 선택한다면?', '["10시간 공부", "10시간 수면"]'::jsonb, 1, '2025-09-26 13:44:15.203', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(3, '내 친구에게 더 빡치는 순간은?', '["일쌤할 때", "약속 늦을 때"]'::jsonb, 1, '2025-09-26 13:44:15.203', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(4, '이상적인 짝꿍은?', '["말 많은 짝꿍", "말 없는 짝꿍"]'::jsonb, 1, '2025-09-26 13:44:15.203', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(5, '수업 시간에 더 힘든건?', '["졸음과의 싸움", "배고픔과의 싸움"]'::jsonb, 1, '2025-09-26 13:44:15.203', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(6, '급식 배식 순서, 차라리 낫다고 생각되는 건?', '["맨 앞에서 기다리기", "맨 뒤에서 남은 거 받기"]'::jsonb, 1, '2025-09-26 13:44:15.203', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(7, '폰에서 평생 하나만 쓸 수 있다면?', '["유튜브", "인스타", "하이틴"]'::jsonb, 1, '2025-09-26 14:23:48.151', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(8, '가장 먼저 사고 싶은 전자기기는?', '["PC", "무선 이어폰", "최신 휴대폰"]'::jsonb, 1, '2025-09-26 14:24:05.148', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(9, '공부가 제일 잘 되는 장소는?', '["집", "독서실", "스터디카페", "그냥 카페"]'::jsonb, 1, '2025-09-26 14:25:22.016', NULL, NULL);
INSERT INTO poll_templates
(id, question, answers, state, created_at, updated_at, deleted_at)
VALUES(10, '더 기쁜 소식은?', '["내 최애 아이돌 그룹의 컴백", "새로운 맛있는 음식 발견"]'::jsonb, 1, '2025-09-26 14:25:55.854', NULL, NULL);

-- Lv1 브론즈 (0~199)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('BRONZE_STAR', '별빛 브론즈', 1, 1, 0, 49),
('BRONZE_MOON', '달빛 브론즈', 2, 2, 50, 99),
('BRONZE_SUN',  '태양 브론즈', 3, 3, 100, 199);

-- Lv2 실버 (200~499)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('SILVER_STAR', '별빛 실버', 1, 4, 200, 299),
('SILVER_MOON', '달빛 실버', 2, 5, 300, 399),
('SILVER_SUN',  '태양 실버', 3, 6, 400, 499);

-- Lv3 골드 (500~999)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('GOLD_STAR', '별빛 골드', 1, 7, 500, 649),
('GOLD_MOON', '달빛 골드', 2, 8, 650, 849),
('GOLD_SUN',  '태양 골드', 3, 9, 850, 999);

-- Lv4 플래티넘 (1000~2999)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('PLATINUM_STAR', '별빛 플래티넘', 1, 10, 1000, 1499),
('PLATINUM_MOON', '달빛 플래티넘', 2, 11, 1500, 2499),
('PLATINUM_SUN',  '태양 플래티넘', 3, 12, 2500, 2999);

-- Lv5 다이아몬드 (3000~4999)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('DIAMOND_STAR', '별빛 다이아몬드', 1, 13, 3000, 3499),
('DIAMOND_MOON', '달빛 다이아몬드', 2, 14, 3500, 4499),
('DIAMOND_SUN',  '태양 다이아몬드', 3, 15, 4500, 4999);

-- Lv6 마스터 (5000~7999)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('MASTER_STAR', '별빛 마스터', 1, 16, 5000, 5999),
('MASTER_MOON', '달빛 마스터', 2, 17, 6000, 6999),
('MASTER_SUN',  '태양 마스터', 3, 18, 7000, 7999);

-- Lv7 그랜드마스터 (8000~9999)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('GRANDMASTER_STAR', '별빛 그랜드마스터', 1, 19, 8000, 8499),
('GRANDMASTER_MOON', '달빛 그랜드마스터', 2, 20, 8500, 9499),
('GRANDMASTER_SUN',  '태양 그랜드마스터', 3, 21, 9500, 9999);

-- Lv8 챌린저 (10000 이상)
INSERT INTO tiers (tier_code, tier_name_kr, division_no, rank_order, min_points, max_points)
VALUES
('CHALLENGER', '챌린저', 1, 22, 10000, 2147483647);




INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(1, '축구', '스포츠', 'Y', 1, '2025-10-15 18:10:40.807', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(2, '농구', '스포츠', 'Y', 1, '2025-10-15 18:11:17.134', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(3, '배구', '스포츠', 'Y', 1, '2025-10-15 18:11:40.219', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(4, '야구', '스포츠', 'Y', 1, '2025-10-15 18:11:43.671', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(5, '스케이팅', '스포츠', 'Y', 1, '2025-10-15 18:11:46.463', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(6, '사이클', '스포츠', 'Y', 1, '2025-10-15 18:11:49.993', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(7, '유도', '스포츠', 'Y', 1, '2025-10-15 18:11:53.780', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(8, '검도', '스포츠', 'Y', 1, '2025-10-15 18:11:56.304', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(9, '탁구', '스포츠', 'Y', 1, '2025-10-15 18:11:58.174', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(10, '클라이밍', '스포츠', 'Y', 1, '2025-10-15 18:12:01.014', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(11, 'K-POP', '음악', 'Y', 1, '2025-10-15 18:12:10.525', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(12, '힙합', '음악', 'Y', 1, '2025-10-15 18:12:14.444', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(13, '발라드', '음악', 'Y', 1, '2025-10-15 18:12:17.114', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(14, '인디음악', '음악', 'Y', 1, '2025-10-15 18:12:20.709', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(15, 'EDM', '음악', 'Y', 1, '2025-10-15 18:12:24.788', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(16, '악기연주', '음악', 'Y', 1, '2025-10-15 18:12:35.871', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(17, '작곡/비트메이킹', '음악', 'Y', 1, '2025-10-15 18:12:40.960', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(18, '넷플릭스 정주행', '영화/드라마', 'Y', 1, '2025-10-15 18:12:51.157', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(19, '공포영화', '영화/드라마', 'Y', 1, '2025-10-15 18:12:56.492', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(20, '로맨스', '영화/드라마', 'Y', 1, '2025-10-15 18:12:59.401', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(21, '액션', '영화/드라마', 'Y', 1, '2025-10-15 18:13:01.025', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(22, '애니메이션', '영화/드라마', 'Y', 1, '2025-10-15 18:13:05.225', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(23, '웹드라마', '영화/드라마', 'Y', 1, '2025-10-15 18:13:08.046', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(24, '리그 오브 레전드', '게임', 'Y', 1, '2025-10-15 18:13:25.123', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(25, '배틀그라운드', '게임', 'Y', 1, '2025-10-15 18:13:33.046', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(26, '마인크래프트', '게임', 'Y', 1, '2025-10-15 18:13:38.884', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(27, '닌텐도', '게임', 'Y', 1, '2025-10-15 18:13:47.104', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(28, '모바일게임', '게임', 'Y', 1, '2025-10-15 18:13:50.360', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(29, '게임 스트리머', '게임', 'Y', 1, '2025-10-15 18:13:54.971', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(30, '독서', '취미/일상', 'Y', 1, '2025-10-15 18:14:01.941', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(31, '그림 그리기', '취미/일상', 'Y', 1, '2025-10-15 18:14:08.932', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(32, '브이로그 보기', '취미/일상', 'Y', 1, '2025-10-15 18:14:13.518', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(33, '필름 카메라', '취미/일상', 'Y', 1, '2025-10-15 18:14:20.236', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(34, '다꾸', '취미/일상', 'Y', 1, '2025-10-15 18:14:22.230', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(35, '요리와 디저트', '취미/일상', 'Y', 1, '2025-10-15 18:14:29.828', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(36, '산책', '취미/일상', 'Y', 1, '2025-10-15 18:14:33.378', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(37, '자전거', '취미/일상', 'Y', 1, '2025-10-15 18:14:35.118', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(38, '과학 좋아함', '학습/진로', 'Y', 1, '2025-10-15 18:14:50.158', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(39, '수학 잘함', '학습/진로', 'Y', 1, '2025-10-15 18:14:54.410', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(40, '다국어 가능', '학습/진로', 'Y', 1, '2025-10-15 18:14:57.652', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(41, '의사', '학습/진로', 'Y', 1, '2025-10-15 18:14:59.914', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(42, '디자이너', '학습/진로', 'Y', 1, '2025-10-15 18:15:02.729', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(43, '유튜버', '학습/진로', 'Y', 1, '2025-10-15 18:15:07.415', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(44, '인스타그램', 'SNS/소통', 'Y', 1, '2025-10-15 18:15:18.378', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(45, '틱톡', 'SNS/소통', 'Y', 1, '2025-10-15 18:15:21.720', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(46, '밈/짤모으기', 'SNS/소통', 'Y', 1, '2025-10-15 18:15:31.520', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(47, '커뮤니티활동', 'SNS/소통', 'Y', 1, '2025-10-15 18:15:46.507', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(48, '채팅 좋아함', 'SNS/소통', 'Y', 1, '2025-10-15 18:15:50.592', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(49, '익명톡 즐김', 'SNS/소통', 'Y', 1, '2025-10-15 18:15:55.544', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(50, '낯가림심함', '성격/기질', 'Y', 1, '2025-10-15 18:16:07.180', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(51, '활발한편', '성격/기질', 'Y', 1, '2025-10-15 18:16:11.039', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(52, '고민잘들어줌', '성격/기질', 'Y', 1, '2025-10-15 18:16:14.978', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(53, 'MBTI토크', '성격/기질', 'Y', 1, '2025-10-15 18:16:25.844', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(54, '감성적', '성격/기질', 'Y', 1, '2025-10-15 18:16:31.253', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(55, '편의점 신상', '음식/장소', 'Y', 1, '2025-10-15 18:16:41.350', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(56, '디저트 맛집', '음식/장소', 'Y', 1, '2025-10-15 18:16:47.444', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(57, '배달앱 탐색', '음식/장소', 'Y', 1, '2025-10-15 18:16:51.937', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(58, '브런치 좋아함', '음식/장소', 'Y', 1, '2025-10-15 18:16:55.748', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(60, '야식러버', '음식/장소', 'Y', 1, '2025-10-15 18:17:04.077', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(59, '카페 탐방', '음식/장소', 'Y', 1, '2025-10-15 18:17:00.212', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(61, '거리', '추천방식', 'Y', 1, '2025-10-15 18:18:13.918', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(62, '관심사', '추천옵션', 'Y', 1, '2025-10-15 18:18:19.989', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(63, '남학생', '추천옵션', 'Y', 1, '2025-10-15 18:18:23.360', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(64, '여학생', '추천옵션', 'Y', 1, '2025-10-15 18:18:26.200', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(65, '동급생', '추천옵션', 'Y', 1, '2025-10-15 18:18:29.738', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(66, '선배', '추천옵션', 'Y', 1, '2025-10-15 18:18:31.929', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(67, '후배', '추천옵션', 'Y', 1, '2025-10-15 18:18:33.374', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(68, '같은 학교', '추천제외', 'Y', 1, '2025-10-15 18:18:45.006', NULL, NULL, NULL, NULL);
INSERT INTO public.interests
(id, topic, category, status, created_id, created_at, updated_id, updated_at, deleted_id, deleted_at)
VALUES(69, '연락처', '추천제외', 'Y', 1, '2025-10-15 18:18:49.203', NULL, NULL, NULL, NULL);

INSERT INTO games (code, name, description, status, created_at)
VALUES
  ('NUMBER_SPEED', '숫자 스피드 게임', '숫자를 빠르게 선택하는 게임', 'ACTIVE', now()),
  ('MEMORY_TEST', '기억력 테스트', '순서를 기억하고 맞추는 게임', 'ACTIVE', now()),
  ('WORD_CHALLENGE', '영단어 챌린지', '영단어 맞추기 게임', 'ACTIVE', now()),
  ('BINGO', '빙고 게임', '빙고판에서 숫자를 맞추는 게임', 'ACTIVE', now());

