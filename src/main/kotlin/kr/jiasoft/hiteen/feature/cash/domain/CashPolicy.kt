package kr.jiasoft.hiteen.feature.cash.domain

enum class CashPolicy(
    val code: String,
    val amount: Int,           // +면 적립, -면 차감
    val dailyLimit: Int? = null,
    val memoTemplate: String   // 기본 메모
) {

    BUY("BUY", 0, null, "상품 구매"),
    CHALLENGE_REWARD("CHALLENGE_REWARD", 0, null, "챌린지 보상"),

    // --- 회원가입 ---
    SIGNUP("SIGNUP", 500, null, "회원가입 보상"),

    // --- 틴스토리 ---
    STORY_POST("STORY_POST", 50, 3, "스토리 글 작성"),
    STORY_COMMENT("STORY_COMMENT", 10, 10, "스토리 댓글 작성"),

    // --- 틴투표 ---
    VOTE_QUESTION("VOTE_QUESTION", 30, 5, "투표 질문 등록"),
    VOTE_COMMENT("VOTE_COMMENT", 10, 10, "투표 댓글 작성"),

    // --- 게임 ---
    GAME_PLAY("GAME_PLAY", -100, null, "게임 플레이 차감"),

    // --- MBTI ---
    MBTI_TEST("MBTI_TEST", -300, null, "MBTI 검사 차감"),

    // --- 친구추천 ---
    FRIEND_RECOMMEND("FRIEND_RECOMMEND", 500, 2, "친구 추천 보상"),
    FRIEND_INVITE("FRIEND_INVITE", 1000, null, "초대한 친구 가입 보상"),

    // --- 출석 ---
    ATTEND_DAY1("ATTEND_DAY1", 100, 1, "1일차 출석"),
    ATTEND_DAY2("ATTEND_DAY2", 100, 1, "2일차 출석"),
    ATTEND_DAY3("ATTEND_DAY3", 200, 1, "3일차 출석"),
    ATTEND_DAY4("ATTEND_DAY4", 200, 1, "4일차 출석"),
    ATTEND_DAY5("ATTEND_DAY5", 300, 1, "5일차 출석"),
    ATTEND_DAY6("ATTEND_DAY6", 300, 1, "6일차 출석"),
    ATTEND_DAY7("ATTEND_DAY7", 500, 1, "7일차 출석"),

    // --- 광고 ---
    AD_REWARD("AD_REWARD", 100, 5, "광고 시청 보상"),

    // --- 결제 충전 ---
    PAYMENT_1000("PAYMENT_1000", 1100, null, "포인트 충전 (1,000원)"),
    PAYMENT_3000("PAYMENT_3000", 3300, null, "포인트 충전 (3,000원)"),
    PAYMENT_5000("PAYMENT_5000", 5500, null, "포인트 충전 (5,000원)"),
    PAYMENT_10000("PAYMENT_10000", 11000, null, "포인트 충전 (10,000원)"),

    // --- ADMIN 지급 ---
    ADMIN("ADMIN", 0, null, "관리자 포인트 지급"),

    // --- 기타 ---
    ETC("ETC", 0, null, "기타"),

    // --- 기타 ---
    TEST("TEST", 100, null, "기타")
}
