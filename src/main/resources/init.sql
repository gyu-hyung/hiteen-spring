INSERT INTO point_rules (
    action_code,
    point,
    daily_cap,
    cooldown_sec,
    description,
    created_id,
    created_at
) VALUES
-- ========================
-- 회원가입
-- ========================
('SIGNUP', 500, NULL, NULL, '회원가입 보상', 1, now()),

-- ========================
-- 틴스토리
-- ========================
('STORY_POST', 50, 3, NULL, '스토리 글 작성', 1, now()),
('STORY_COMMENT', 10, 10, NULL, '스토리 댓글 작성', 1, now()),

-- ========================
-- 틴투표
-- ========================
('VOTE_QUESTION', 30, 5, NULL, '투표 질문 등록', 1, now()),
('VOTE_COMMENT', 10, 10, NULL, '투표 댓글 작성', 1, now()),

-- ========================
-- 게임
-- ========================
('GAME_PLAY', -100, NULL, NULL, '게임 플레이 차감', 1, now()),

-- ========================
-- MBTI
-- ========================
('MBTI_TEST', -300, NULL, NULL, 'MBTI 검사 차감', 1, now()),

-- ========================
-- 친구 추천
-- ========================
('FRIEND_RECOMMEND', 500, 2, NULL, '친구 추천 보상', 1, now()),
('FRIEND_INVITE', 1000, NULL, NULL, '초대한 친구 가입 보상', 1, now()),

-- ========================
-- 출석
-- ========================
('ATTEND_DAY1', 100, 1, NULL, '1일차 출석', 1, now()),
('ATTEND_DAY2', 100, 1, NULL, '2일차 출석', 1, now()),
('ATTEND_DAY3', 200, 1, NULL, '3일차 출석', 1, now()),
('ATTEND_DAY4', 200, 1, NULL, '4일차 출석', 1, now()),
('ATTEND_DAY5', 300, 1, NULL, '5일차 출석', 1, now()),
('ATTEND_DAY6', 300, 1, NULL, '6일차 출석', 1, now()),
('ATTEND_DAY7', 500, 1, NULL, '7일차 출석', 1, now()),

-- ========================
-- 광고
-- ========================
('AD_REWARD', 100, 5, NULL, '광고 시청 보상', 1, now()),

-- ========================
-- 결제 충전
-- ========================
('PAYMENT_1000', 1100, NULL, NULL, '포인트 충전 (1,000원)', 1, now()),
('PAYMENT_3000', 3300, NULL, NULL, '포인트 충전 (3,000원)', 1, now()),
('PAYMENT_5000', 5500, NULL, NULL, '포인트 충전 (5,000원)', 1, now()),
('PAYMENT_10000', 11000, NULL, NULL, '포인트 충전 (10,000원)', 1, now()),

-- ========================
-- 관리자 / 기타
-- ========================
('ADMIN', 0, NULL, NULL, '관리자 포인트 지급', 1, now()),
('ETC', 0, NULL, NULL, '기타', 1, now()),
('TEST', 100, NULL, NULL, '기타', 1, now())

ON CONFLICT (action_code)
DO UPDATE SET
    point        = EXCLUDED.point,
    daily_cap    = EXCLUDED.daily_cap,
    cooldown_sec = EXCLUDED.cooldown_sec,
    description  = EXCLUDED.description,
    updated_id   = 1,
    updated_at   = now();
