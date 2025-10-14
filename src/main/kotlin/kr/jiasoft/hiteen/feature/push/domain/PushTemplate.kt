package kr.jiasoft.hiteen.feature.push.domain

/**
 * 푸시 템플릿 메시지에 변수를 동적으로 치환해주는 함수
 */
fun PushTemplate.buildPushData(vararg pairs: Pair<String, Any?>): Map<String, Any> {
    val params = pairs.toMap()
    var formattedMessage = message
    params.forEach { (key, value) ->
        formattedMessage = formattedMessage.replace("{$key}", value.toString())
    }
    return mapOf(
        "code" to code,
        "title" to title,
        "message" to formattedMessage,
        "silent" to false
    )
}


/**
 * 🔔 앱 전역 알림 종류 정의
 * 모든 푸시 메시지를 한 곳에서 관리
 */
enum class PushTemplate(
    val code: String,
    val title: String,
    val message: String
) {
    /**
     * 친구 요청
     */
    FRIEND_REQUEST(
        code = "FRIEND_REQUEST",
        title = "친구 요청 💌",
        message = "{nickname}님이 친구 요청을 보냈어요~"
    ),

    /**
     * 친구 요청 숭인
     * */
    FRIEND_ACCEPT(
        code = "FRIEND_ACCEPT",
        title = "친구 요청 승인 💌",
        message = "{nickname}님이 친구 요청을 승인했어요~"
    ),


    /**
     * 팔로우 요청 알림
     */
    FOLLOW_REQUEST(
        code = "FOLLOW",
        title = "새로운 팔로우 👀",
        message = "{nickname}님이 당신을 팔로우하기 시작했어요~"
    ),


    /**
     * 팔로우 요청 수락 알림
     */
    FOLLOW_ACCEPT(
        code = "FOLLOW",
        title = "새로운 팔로우 👀",
        message = "{nickname}님이 당신의 팔로우 요청을 수락했어요~"
    ),


    /**
     * 새 글 등록 알림
     */
    NEW_POST(
        code = "NEW_POST",
        title = "새 글 등록 ✍️",
        message = "{nickname}님이 새 글을 등록했어요~"
    ),

    /**
     * 핀 등록 알림
     */
    PIN_REGISTER(
        code = "PIN_REGISTER",
        title = "핀 등록 알림 📍",
        message = "{nickname}님이 새로운 핀을 등록했어요~"
    ),

    /**
     * 게시글 댓글 알림
     */
    BOARD_COMMENT(
        code = "BOARD_COMMENT",
        title = "틴스토리 댓글 알림 💬",
        message = "{nickname}님이 새로운 댓글을 남겼어요~"
    ),


    /**
     * 투표 댓글 알림
     */
    VOTE_COMMENT(
        code = "VOTE_COMMENT",
        title = "틴투표 댓글 알림 💬",
        message = "{nickname}님이 새로운 댓글을 남겼어요~"
    ),

    /**
     * 채팅 알림
     */
    CHAT_MESSAGE(
        code = "CHAT_MESSAGE",
        title = "새로운 채팅 💬",
        message = "{nickname}님이 새로운 메시지를 보냈어요~"
    );
}


