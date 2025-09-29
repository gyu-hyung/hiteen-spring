-- ========================
-- hiteen DDL
-- ========================

CREATE EXTENSION IF NOT EXISTS pgcrypto; -- for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS citext;   -- case-insensitive text
CREATE EXTENSION IF NOT EXISTS cube;
CREATE EXTENSION IF NOT EXISTS earthdistance;



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
  exp_points        integer DEFAULT 0,
  tier_id           bigint REFERENCES tiers(id);
  asset_uid         uuid, -- REFERENCES assets(uid),
  school_id         bigint,
  grade             varchar(30),
  gender            varchar(30),
  birthday		    date,
  profile_decoration_code VARCHAR(50)
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
--CREATE UNIQUE INDEX users_email_key
--    ON users (lower(email))
--    WHERE deleted_at IS NULL;

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
  memo          text
);


-- ========================
-- 사용 사진
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
  id           bigserial PRIMARY KEY,
  tier_code    varchar(30) NOT NULL, -- "BRONZE", "SILVER", "GOLD"
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
  friend_location_mode  varchar(20) DEFAULT 'PUBLIC'
  created_at            timestamptz DEFAULT now(),
  updated_at            timestamptz,
  deleted_at            timestamptz,
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
-- 투표 > 좋아요
-- ========================
CREATE TABLE poll_likes (
  id         bigserial PRIMARY KEY,
  poll_id    bigint NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
  user_id    bigint NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
  created_at timestamptz DEFAULT now(),
--  updated_at timestamptz,
--  deleted_at timestamptz,
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
    created_at TIMESTAMP,                      -- 전송일시
    updated_at TIMESTAMP,                      -- 수정일시
    deleted_at TIMESTAMP                       -- 삭제일시
);



-- ========================
-- 푸시 전송상세
-- ========================
CREATE TABLE push_detail (
    id BIGSERIAL PRIMARY KEY,                  -- 상세번호
    push_id BIGINT NOT NULL REFERENCES tb_push(id), -- 전송번호 (FK)
    user_id BIGINT,                            -- 수신자 회원번호
    device_os VARCHAR(50),                     -- 디바이스 OS
    device_token VARCHAR(300),                 -- 디바이스 토큰
    phone VARCHAR(20),                         -- 전화번호
    multicast_id VARCHAR(300),                 -- 다중ID
    message_id VARCHAR(300),                   -- 메시지ID
    registration_id VARCHAR(300),              -- 인증ID
    error TEXT,                                -- 에러내용
    success SMALLINT NOT NULL DEFAULT 0,       -- 성공여부 (0/1)
    created_at TIMESTAMP,                      -- 전송일시
    updated_at TIMESTAMP,                      -- 수정일시
    deleted_at TIMESTAMP                       -- 삭제일시
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
--  poll_templates,
--  poll_comment_likes,
--  poll_comments,
--  poll_users,
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
--  school_classes,
--  schools,
--  tiers,
--  assets,
--  interest_user,
--  interests,
--  code_assets,
--  codes,
--  user_details,
--  users,
--  sms,
--  sms_details,
--  sms_auth
--CASCADE;



INSERT INTO users (uid,username,email,nickname,"password","role",address,detail_address,phone,mood,tier,asset_uid,school_id,grade,gender,birthday,created_id,created_at,updated_id,updated_at,deleted_id,deleted_at) VALUES
	 ('6f9b90d6-96ca-49de-b9c2-b123e51ca7db'::uuid,'chat1','qwe@chat','닉넴1','$2a$10$X2FQYkcsynbJJwExkHDiMenGylpHGW9cFWEgugqeQYW4ZniYoPolG','USER','광주광역시 북구 시청로 1','2층','01095393637','기분좋음','브론즈 1','a0e9f441-3669-4cfb-aac5-1c38533d87c1'::uuid,1,'1','M','1999-12-01',NULL,'2025-09-09 14:26:44.586',NULL,NULL,NULL,NULL),
	 ('f55db2b7-c8f3-4ebf-94c7-577bc4a3939b'::uuid,'chat2','qwe@chat','닉넴2','$2a$10$J1ZORvmaZji6btaLrdxv0ewHHjO2i8ZsmOtR3MPz1VGrS/8RNkgyG','USER','광주광역시 북구 시청로 1','2층','01022222222','기분좋음','브론즈 1','9524f629-4885-4dcf-bc5c-1f02391a1f8b'::uuid,1,'1','M','1999-12-01',NULL,'2025-09-09 14:27:20.574',NULL,NULL,NULL,NULL),
	 ('c2605174-08b8-4879-acab-3f7f122bb2ed'::uuid,'chat3','qwe@chat','닉넴3','$2a$10$iVcqM0RGK4uGwkDfHUr8NOaSwbwrXRPpowXpe4BL6XTFcPfL2bc/6','USER','광주광역시 북구 시청로 1','2층','01033333333','기분좋음','브론즈 1','f26d080f-6f9c-485a-a8fd-f1850d217a2f'::uuid,1,'1','M','1999-12-01',NULL,'2025-09-09 14:28:41.477',NULL,NULL,NULL,NULL),
	 ('6fae6813-73bb-47c0-a775-af1c6ca9a79b'::uuid,'chat4','qwe@chat','닉넴4','$2a$10$CUooWUv/UP4HlgMYzW4IMujmSmUkEn4lMoP7GTqT4YEy0hfaQaW0.','USER','광주광역시 북구 시청로 1','2층','01044444444','기분좋음','브론즈 1','f0bceba2-e02b-4706-9f0f-743616d9d911'::uuid,1,'1','M','1999-12-01',NULL,'2025-09-09 14:28:54.742',NULL,NULL,NULL,NULL),
	 ('c264013d-bb1d-4d66-8d34-10962c022056'::uuid,'chat5','qwe@chat','닉넴5','$2a$10$WZ67oFFBEFypJ28HGxudzuhFu3tS6N8P4.HKKLPYdiaCh7tsZvDva','USER','광주광역시 북구 시청로 1','2층','01055555555','기분좋음','브론즈 1','f580e8e8-adee-4285-b181-3fed545e7be0'::uuid,1,'1','M','1999-12-01',NULL,'2025-09-09 14:29:06.591',NULL,NULL,NULL,NULL);



-- Lv1 브론즈 (0~199)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('BRONZE_STAR', 1, 1, 0, 49),         -- 별빛 브론즈
('BRONZE_MOON', 2, 2, 50, 99),        -- 달빛 브론즈
('BRONZE_SUN', 3, 3, 100, 199);       -- 태양 브론즈

-- Lv2 실버 (200~499)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('SILVER_STAR', 1, 4, 200, 299),      -- 별빛 실버
('SILVER_MOON', 2, 5, 300, 399),      -- 달빛 실버
('SILVER_SUN', 3, 6, 400, 499);       -- 태양 실버

-- Lv3 골드 (500~999)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('GOLD_STAR', 1, 7, 500, 649),        -- 별빛 골드
('GOLD_MOON', 2, 8, 650, 849),        -- 달빛 골드
('GOLD_SUN', 3, 9, 850, 999);         -- 태양 골드

-- Lv4 플래티넘 (1000~2999)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('PLATINUM_STAR', 1, 10, 1000, 1499),   -- 별빛 플래티넘
('PLATINUM_MOON', 2, 11, 1500, 2499),   -- 달빛 플래티넘
('PLATINUM_SUN', 3, 12, 2500, 2999);    -- 태양 플래티넘

-- Lv5 다이아몬드 (3000~4999)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('DIAMOND_STAR', 1, 13, 3000, 3499),   -- 별빛 다이아몬드
('DIAMOND_MOON', 2, 14, 3500, 4499),   -- 달빛 다이아몬드
('DIAMOND_SUN', 3, 15, 4500, 4999);    -- 태양 다이아몬드

-- Lv6 마스터 (5000~7999)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('MASTER_STAR', 1, 16, 5000, 5999),    -- 별빛 마스터
('MASTER_MOON', 2, 17, 6000, 6999),    -- 달빛 마스터
('MASTER_SUN', 3, 18, 7000, 7999);     -- 태양 마스터

-- Lv7 그랜드마스터 (8000~9999)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('GRANDMASTER_STAR', 1, 19, 8000, 8499),  -- 별빛 그랜드마스터
('GRANDMASTER_MOON', 2, 20, 8500, 9499),  -- 달빛 그랜드마스터
('GRANDMASTER_SUN', 3, 21, 9500, 9999);   -- 태양 그랜드마스터

-- Lv8 챌린저 (10000 이상)
INSERT INTO tiers (tier_code, division_no, rank_order, min_points, max_points)
VALUES
('CHALLENGER', 1, 22, 10000, 2147483647); -- 챌린저

