package kr.jiasoft.hiteen.feature.push.domain

import kr.jiasoft.hiteen.util.KoreanPostPosition


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
        message = "{nickname_iga} 너랑 친구가 되고 싶어해 😊"
    ),

    /**
     * 친구 요청 승인
     */
    FRIEND_ACCEPT(
        code = "FRIEND_ACCEPT",
        title = "친구 요청 승인 💌",
        message = "{nickname_iga} 친구 요청을 수락했어 🤭"
    ),


    /**
     * 팔로우 요청 알림
     */
    FOLLOW_REQUEST(
        code = "FOLLOW_REQUEST",
        title = "새로운 팔로우 👀",
        message = "{nickname_iga} 나를 팔로우하려고 해 😚"
    ),

    /**
     * 팔로우 요청 수락 알림
     */
    FOLLOW_ACCEPT(
        code = "FOLLOW_ACCEPT",
        title = "팔로우 수락 🥰",
        message = "{nickname_iga} 내 팔로우를 수락했어 🥰"
    ),


    /**
     * 새 글 등록 알림
     */
    NEW_POST(
        code = "NEW_POST",
        title = "새 글 등록 알림 🔔",
        message = "방금 새로운 글이 올라왔어~ 🔔"
    ),

    /**
     * 핀 등록 알림
     */
    PIN_REGISTER(
        code = "PIN_REGISTER",
        title = "핀 등록 알림 📍",
        message = "{nickname_iga} 지금 핀을 등록했어 📍"
    ),

    /**
     * 게시글 댓글 알림
     */
    BOARD_COMMENT(
        code = "BOARD_COMMENT",
        title = "틴스토리 댓글 👀",
        message = "내 게시글에 댓글이 달렸어 👀"
    ),


    /**
     * 투표 댓글 알림
     */
    VOTE_COMMENT(
        code = "VOTE_COMMENT",
        title = "틴투표 댓글 알림 💬",
//        message = "{nickname_iga} 새로운 댓글을 남겼어~"
        message = "내 투표에 댓글이 달렸어 👀"
    ),

    /**
     * 채팅 알림
     */
    CHAT_MESSAGE(
        code = "CHAT_MESSAGE",
        title = "새로운 채팅 💬",
        message = "{nickname_iga} 새로운 메시지를 보냈어~"
    ),

    /**
     * 선물 알림
     */
    GIFT_MESSAGE(
        code = "GIFT_MESSAGE",
        title = "새로운 선물 도착! 🎁",
        message = "새로운 선물 도착! 🎁"
    ),


    ;

    /**
     * 🔹 푸시 템플릿 메시지에 변수를 동적으로 치환
     * 🔹 nickname이 있으면 조사 파생 변수 자동 생성
     */
    fun buildPushData(vararg pairs: Pair<String, Any?>): Map<String, Any> {
        val params = pairs.toMap().toMutableMap()

        // 🔥 nickname 조사 자동 생성
        val nickname = params["nickname"]?.toString()
        if (!nickname.isNullOrBlank()) {
            params["nickname_iga"] =
                KoreanPostPosition.attach(nickname, KoreanPostPosition.Type.I_GA)

            params["nickname_eunneun"] =
                KoreanPostPosition.attach(nickname, KoreanPostPosition.Type.EUN_NEUN)

            params["nickname_eulreul"] =
                KoreanPostPosition.attach(nickname, KoreanPostPosition.Type.EUL_REUL)
        }

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


}


