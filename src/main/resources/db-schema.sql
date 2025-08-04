drop table tb_users

select * from tb_users tu

select * from tb_users where name = 'test'

SELECT oid, typname FROM pg_catalog.pg_type WHERE typname IN ('hstore')

INSERT INTO tb_users (name, password, role) VALUES ('test', '$2a$10$IDtbXc6JkDM.1P8dFa8nEOymI/3riW9kmbZ07eX1AA7KkVFP6Cz8i', 'USER');

UPDATE tb_users SET password = '$2a$10$IrnBhQ1bkPkCMPmX6CxCcO2CPlZC/GpMmMS8.EaiYa0C07PfUN5QS' WHERE name = 'test';

INSERT INTO public.tb_users
(id, username, "password", "role")
VALUES(4, 'test', '$2a$10$IrnBhQ1bkPkCMPmX6CxCcO2CPlZC/GpMmMS8.EaiYa0C07PfUN5QS', 'USER');

INSERT INTO public.tb_users
(id, username, email, nickname, "password", "role", created_at, updated_at)
VALUES(1, 'test1', 'qwe@d', '닉넴', '$2a$10$JR/9b1q.XeFYOxJh7krOyePc/O85aLWz41AD9IFcRpzlSKYMWHwKC', 'USER', '2025-08-04 13:58:37.796', NULL);


CREATE TABLE IF NOT EXISTS tb_users (
    id BIGSERIAL PRIMARY KEY,                                -- 회원번호 (PK)
    username VARCHAR(100),                                       -- 회원명
    email VARCHAR(100),                                       -- 이메일
    nickname VARCHAR(100),                                       -- 닉네임
    password VARCHAR(100),                                       -- 비밀번호
    role VARCHAR(100),                                       -- 권한
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT null
);

SELECT tb_users.id, tb_users.username, tb_users.password, tb_users.role FROM tb_users WHERE tb_users.username = 'test'
-- -------------------------------------------------
-- 컬럼별 COMMENT 추가
-- -------------------------------------------------
COMMENT ON TABLE tb_users IS '사용자 테이블';
COMMENT ON COLUMN tb_users.id IS '회원번호 (PK)';
COMMENT ON COLUMN tb_users.uid IS '회원UID (외부 시스템용)';
COMMENT ON COLUMN tb_users.type IS '회원그룹 (User/Admin)';
COMMENT ON COLUMN tb_users.phone IS '휴대폰번호';
COMMENT ON COLUMN tb_users.teen_id IS '틴 아이디 (사용불가)';
COMMENT ON COLUMN tb_users.password IS '비밀번호 (암호화)';
COMMENT ON COLUMN tb_users.name IS '회원명';
COMMENT ON COLUMN tb_users.birthday IS '생년월일 (YYYY-MM-DD)';
COMMENT ON COLUMN tb_users.gender IS '성별 (M, F)';
COMMENT ON COLUMN tb_users.school_year IS '학년도';
COMMENT ON COLUMN tb_users.school_id IS '학교번호';
COMMENT ON COLUMN tb_users.class_id IS '학급번호';
COMMENT ON COLUMN tb_users.school_type IS '학교구분 (초/중/고/기타)';
COMMENT ON COLUMN tb_users.school_name IS '학교명';
COMMENT ON COLUMN tb_users.class_name IS '학급명 (예: 3학년 3반)';
COMMENT ON COLUMN tb_users.grade IS '학년 (예: 3학년)';
COMMENT ON COLUMN tb_users.auth_school IS '학교 인증여부 (0/1)';
COMMENT ON COLUMN tb_users.auth_class IS '반친구 인증여부 (0/1)';
COMMENT ON COLUMN tb_users.image IS '회원 이미지 파일 UID';
COMMENT ON COLUMN tb_users.mbti IS 'MBTI';
COMMENT ON COLUMN tb_users.vote IS '받은 투표수';
COMMENT ON COLUMN tb_users.point IS '포인트';
COMMENT ON COLUMN tb_users.sido IS '시/도';
COMMENT ON COLUMN tb_users.my_teens IS '보유 틴갯수';
COMMENT ON COLUMN tb_users.teens IS '받은 틴갯수';
COMMENT ON COLUMN tb_users.interests IS '관심사 (콤마 구분)';
COMMENT ON COLUMN tb_users.invite_code IS '초대코드';
COMMENT ON COLUMN tb_users.invite_joins IS '초대 후 가입자수';
COMMENT ON COLUMN tb_users.poll_votes IS '받은 α-투표수';
COMMENT ON COLUMN tb_users.poll_comments IS '받은 α-투표 댓글수';
COMMENT ON COLUMN tb_users.status IS '가입상태 (0:대기, 1:승인, 2:차단, 3:탈퇴)';
COMMENT ON COLUMN tb_users.created_at IS '가입일시';
COMMENT ON COLUMN tb_users.updated_at IS '수정일시';
COMMENT ON COLUMN tb_users.deleted_at IS '삭제일시';

-- -------------------------------------------------
-- 주요 컬럼 인덱스 (조회 자주 하는 컬럼 위주)
-- -------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_tb_users_uid ON tb_users(uid);
CREATE INDEX IF NOT EXISTS idx_tb_users_phone ON tb_users(phone);
CREATE INDEX IF NOT EXISTS idx_tb_users_type ON tb_users(type);
CREATE INDEX IF NOT EXISTS idx_tb_users_teen_id ON tb_users(teen_id);
CREATE INDEX IF NOT EXISTS idx_tb_users_name ON tb_users(name);
CREATE INDEX IF NOT EXISTS idx_tb_users_school_id ON tb_users(school_id);
CREATE INDEX IF NOT EXISTS idx_tb_users_class_id ON tb_users(class_id);
CREATE INDEX IF NOT EXISTS idx_tb_users_invite_code ON tb_users(invite_code);
CREATE INDEX IF NOT EXISTS idx_tb_users_status ON tb_users(status);













CREATE TABLE IF NOT EXISTS tb_users (
    id BIGSERIAL PRIMARY KEY,                                -- 회원번호 (PK)
    uid VARCHAR(100),                                        -- 회원UID
    type VARCHAR(20) NOT NULL,                               -- 회원그룹 (User/Admin)
    phone VARCHAR(200) NOT NULL,                             -- 휴대폰번호
    teen_id VARCHAR(50),                                     -- 틴 아이디
    password VARCHAR(255),                                   -- 비밀번호
    name VARCHAR(100),                                       -- 회원명
    birthday DATE,                                           -- 생년월일
    gender VARCHAR(1),                                       -- 성별 (M/F)
    school_year SMALLINT,                                    -- 학년도
    school_id INTEGER,                                       -- 학교번호
    class_id BIGINT,                                         -- 학급번호
    school_type SMALLINT,                                    -- 학교구분
    school_name VARCHAR(50),                                 -- 학교명
    class_name VARCHAR(100),                                 -- 학급명
    grade VARCHAR(50),                                       -- 학년
    auth_school SMALLINT NOT NULL DEFAULT 0,                 -- 학교 인증여부
    auth_class SMALLINT NOT NULL DEFAULT 0,                  -- 반친구 인증여부
    image VARCHAR(100),                                      -- 회원 이미지 파일 UID
    mbti CHAR(4),                                            -- MBTI
    vote INTEGER NOT NULL DEFAULT 0,                         -- 받은 투표수
    point INTEGER NOT NULL DEFAULT 0,                        -- 포인트
    sido VARCHAR(20),                                        -- 시/도
    my_teens INTEGER NOT NULL DEFAULT 0,                     -- 보유 틴갯수
    teens INTEGER NOT NULL DEFAULT 0,                        -- 받은 틴갯수
    interests VARCHAR(255),                                  -- 관심사
    invite_code VARCHAR(20),                                 -- 초대코드
    invite_joins INTEGER NOT NULL DEFAULT 0,                 -- 초대 후 가입자수
    poll_votes INTEGER NOT NULL DEFAULT 0,                   -- 받은 α-투표수
    poll_comments INTEGER NOT NULL DEFAULT 0,                -- 받은 α-투표 댓글수
    status SMALLINT NOT NULL DEFAULT 0,                      -- 가입상태
    created_at TIMESTAMP,                                    -- 가입일시
    updated_at TIMESTAMP,                                    -- 수정일시
    deleted_at TIMESTAMP                                     -- 삭제일시
);

-- -------------------------------------------------
-- 컬럼별 COMMENT 추가
-- -------------------------------------------------
COMMENT ON TABLE tb_users IS '사용자 테이블';
COMMENT ON COLUMN tb_users.id IS '회원번호 (PK)';
COMMENT ON COLUMN tb_users.uid IS '회원UID (외부 시스템용)';
COMMENT ON COLUMN tb_users.type IS '회원그룹 (User/Admin)';
COMMENT ON COLUMN tb_users.phone IS '휴대폰번호';
COMMENT ON COLUMN tb_users.teen_id IS '틴 아이디 (사용불가)';
COMMENT ON COLUMN tb_users.password IS '비밀번호 (암호화)';
COMMENT ON COLUMN tb_users.name IS '회원명';
COMMENT ON COLUMN tb_users.birthday IS '생년월일 (YYYY-MM-DD)';
COMMENT ON COLUMN tb_users.gender IS '성별 (M, F)';
COMMENT ON COLUMN tb_users.school_year IS '학년도';
COMMENT ON COLUMN tb_users.school_id IS '학교번호';
COMMENT ON COLUMN tb_users.class_id IS '학급번호';
COMMENT ON COLUMN tb_users.school_type IS '학교구분 (초/중/고/기타)';
COMMENT ON COLUMN tb_users.school_name IS '학교명';
COMMENT ON COLUMN tb_users.class_name IS '학급명 (예: 3학년 3반)';
COMMENT ON COLUMN tb_users.grade IS '학년 (예: 3학년)';
COMMENT ON COLUMN tb_users.auth_school IS '학교 인증여부 (0/1)';
COMMENT ON COLUMN tb_users.auth_class IS '반친구 인증여부 (0/1)';
COMMENT ON COLUMN tb_users.image IS '회원 이미지 파일 UID';
COMMENT ON COLUMN tb_users.mbti IS 'MBTI';
COMMENT ON COLUMN tb_users.vote IS '받은 투표수';
COMMENT ON COLUMN tb_users.point IS '포인트';
COMMENT ON COLUMN tb_users.sido IS '시/도';
COMMENT ON COLUMN tb_users.my_teens IS '보유 틴갯수';
COMMENT ON COLUMN tb_users.teens IS '받은 틴갯수';
COMMENT ON COLUMN tb_users.interests IS '관심사 (콤마 구분)';
COMMENT ON COLUMN tb_users.invite_code IS '초대코드';
COMMENT ON COLUMN tb_users.invite_joins IS '초대 후 가입자수';
COMMENT ON COLUMN tb_users.poll_votes IS '받은 α-투표수';
COMMENT ON COLUMN tb_users.poll_comments IS '받은 α-투표 댓글수';
COMMENT ON COLUMN tb_users.status IS '가입상태 (0:대기, 1:승인, 2:차단, 3:탈퇴)';
COMMENT ON COLUMN tb_users.created_at IS '가입일시';
COMMENT ON COLUMN tb_users.updated_at IS '수정일시';
COMMENT ON COLUMN tb_users.deleted_at IS '삭제일시';

-- -------------------------------------------------
-- 주요 컬럼 인덱스 (조회 자주 하는 컬럼 위주)
-- -------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_tb_users_uid ON tb_users(uid);
CREATE INDEX IF NOT EXISTS idx_tb_users_phone ON tb_users(phone);
CREATE INDEX IF NOT EXISTS idx_tb_users_type ON tb_users(type);
CREATE INDEX IF NOT EXISTS idx_tb_users_teen_id ON tb_users(teen_id);
CREATE INDEX IF NOT EXISTS idx_tb_users_name ON tb_users(name);
CREATE INDEX IF NOT EXISTS idx_tb_users_school_id ON tb_users(school_id);
CREATE INDEX IF NOT EXISTS idx_tb_users_class_id ON tb_users(class_id);
CREATE INDEX IF NOT EXISTS idx_tb_users_invite_code ON tb_users(invite_code);
CREATE INDEX IF NOT EXISTS idx_tb_users_status ON tb_users(status);
