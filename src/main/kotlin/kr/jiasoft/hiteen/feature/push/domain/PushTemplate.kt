package kr.jiasoft.hiteen.feature.push.domain

import kr.jiasoft.hiteen.feature.user.domain.PushItemType
import kr.jiasoft.hiteen.util.KoreanPostPosition


/**
 * 🔔 앱 전역 알림 종류 정의
 * 모든 푸시 메시지를 한 곳에서 관리
 */
enum class PushTemplate(
    val code: String,
    val title: String,
    val message: String,
    val itemType: PushItemType? = null,
    val group: PushTemplateGroup? = null,
) {
    /**
     * 관리자 발송(커스텀)
     * - 관리자 페이지에서 임의 title/message로 발송할 때 사용
     * - 분류/통계용 코드로만 쓰고, 실제 title/message는 발송 시 덮어씀
     */
    ADMIN_SEND(
        code = "ADMIN_SEND",
        title = "관리자 알림",
        message = "관리자 발송 알림",
        itemType = PushItemType.ALL,
        group = PushTemplateGroup.ETC,
    ),

    /**
     * 친구 요청
     */
    FRIEND_REQUEST(
        code = "FRIEND_REQUEST",
        title = "친구 요청 💌",
        message = "{nickname_iga} 너랑 친구가 되고 싶어해 😊",
        itemType = PushItemType.FRIEND,
        group = PushTemplateGroup.FRIEND,
    ),

    /**
     * 친구 요청 승인
     */
    FRIEND_ACCEPT(
        code = "FRIEND_ACCEPT",
        title = "친구 요청 승인 💌",
        message = "{nickname_iga} 친구 요청을 수락했어 🤭",
        itemType = PushItemType.FRIEND,
        group = PushTemplateGroup.FRIEND,
    ),


    /**
     * 팔로우 요청 알림
     */
    FOLLOW_REQUEST(
        code = "FOLLOW_REQUEST",
        title = "새로운 팔로우 👀",
        message = "{nickname_iga} 나를 팔로우하려고 해 😚",
        itemType = PushItemType.FOLLOW,
        group = PushTemplateGroup.FOLLOW,
    ),

    /**
     * 팔로우 요청 수락 알림
     */
    FOLLOW_ACCEPT(
        code = "FOLLOW_ACCEPT",
        title = "팔로우 수락 🥰",
        message = "{nickname_iga} 내 팔로우를 수락했어 🥰",
        itemType = PushItemType.FOLLOW,
        group = PushTemplateGroup.FOLLOW,
    ),


    /**
     * 새 글 등록 알림
     * boardUid
     */
    NEW_POST(
        code = "NEW_POST",
        title = "새 글 등록 알림 🔔",
        message = "{nickname_iga} 새로운 틴스토리를 등록했어 🔔",
        itemType = PushItemType.NEW_POST,
        group = PushTemplateGroup.TEEN_STORY,
    ),


    /**
     * 새 글 등록 알림
     * boardUid
     */
    NEW_VOTE(
        code = "NEW_VOTE",
        title = "틴투표 등록 알림 🔔",
        message = "{nickname_iga} 새로운 틴투표를 등록했어 🔔",
        itemType = PushItemType.NEW_POST,
        group = PushTemplateGroup.TEEN_VOTE,
    ),


    /**
     * 핀 등록 알림
     */
    PIN_REGISTER(
        code = "PIN_REGISTER",
        title = "핀 등록 알림 📍",
        message = "{nickname_iga} 지금 핀을 등록했어 📍",
        PushItemType.PIN_ALERT,
        group = PushTemplateGroup.PIN,
    ),


    /**
     * 게시글 댓글 알림
     */
    BOARD_COMMENT(
        code = "BOARD_COMMENT",
        title = "틴스토리 댓글 👀",
        message = "{nickname_iga} 내 게시글에 새로운 댓글을 남겼어 👀",
        itemType = PushItemType.COMMENT_ALERT,
        group = PushTemplateGroup.COMMENT,
    ),

    /**
     * 게시글 대댓글 알림
     */
    BOARD_REPLY(
        code = "BOARD_REPLY",
        title = "틴스토리 대댓글 👀",
        message = "{nickname_iga} 내 댓글에 새로운 댓글을 남겼어 👀",
        itemType = PushItemType.COMMENT_ALERT,
        group = PushTemplateGroup.COMMENT,
    ),

    /**
     * 투표 댓글 알림
     */
    VOTE_COMMENT(
        code = "VOTE_COMMENT",
        title = "틴투표 댓글 알림 💬",
        message = "{nickname_iga} 내 투표에 새로운 댓글을 남겼어 👀",
        itemType = PushItemType.COMMENT_ALERT,
        group = PushTemplateGroup.COMMENT,
    ),

    /**
     * 투표 답글 알림
     */
    VOTE_REPLY(
        code = "VOTE_REPLY",
        title = "틴투표 답글 알림 💬",
        message = "{nickname_iga} 내 댓글에 새로운 댓글을 남겼어 👀",
        itemType = PushItemType.COMMENT_ALERT,
        group = PushTemplateGroup.COMMENT,
    ),


    /**
     * 채팅 알림
     * roomUid
     */
    CHAT_MESSAGE(
        code = "CHAT_MESSAGE",
        title = "새로운 채팅 💬",
//        message = "{nickname_iga} 새로운 메시지를 보냈어~"
        message = "{chat_message}",
        itemType = PushItemType.CHAT_MESSAGE,
    ),


    /**
     * 선물 알림
     * giftUid
     */
    GIFT_MESSAGE(
        code = "GIFT_MESSAGE",
        title = "새로운 선물 도착! 🎁",
        message = "새로운 선물 도착! 🎁",
        group = PushTemplateGroup.GIFT_SHOP,
    ),


    /**
     * 시즌 생성 알림
     * */
    SEASON_CREATE(
        code = "SEASON_CREATE",
        title = "새로운 시즌이 시작되었어요! 🌟",
        message = "새로운 시즌이 시작되었어요! 지난 시즌 결과를 확인해보세요! 🌟",
        group = PushTemplateGroup.GAME,
    ),


    /**
     * 랭킹 다운 알림
     * 친구에게만
     * */
    RANKING_DOWN(
        code = "RANKING_DOWN",
        title = "랭킹이 하락했어요! 📉",
        message = "누군가 당신의 랭킹을 추월했어요! 신기록에 도전해보세요!📉",
        group = PushTemplateGroup.GAME,
    ),


    /**
     * 랭킹 보상 도착
     * */
    RANKING_REWARD(
        code = "RANKING_REWARD",
        title = "랭킹 보상이 도착했어요! 🎉",
        message = "이번 시즌 랭킹 보상이 도착했어요! 확인해보세요! 🎉",
        group = PushTemplateGroup.GAME,
    ),


    /**
     * 이벤트 알림
     * boardUid
     * */
    EVENT_NOTIFICATION(
        code = "EVENT_NOTIFICATION",
        title = "새로운 이벤트 소식! 🎊",
        message = "{event_name} 이벤트가 시작되었어요! 놓치지 마세요! 🎊",
        group = PushTemplateGroup.ETC,
    ),


    /**
     * 오늘의 친구 추천 시간 알림
     * */
    DAILY_FRIEND_SUGGESTION(
        code = "DAILY_FRIEND_SUGGESTION",
        title = "오늘의 친구 추천 시간이 왔어요! 🤝",
        message = "새로운 친구를 만나보세요! 오늘의 친구 추천 시간이 시작되었어요! 🤝",
        group = PushTemplateGroup.FRIEND,
    ),

    /**
     * 게임 친구 랭킹 변동 알림
     */
    GAME_FRIEND_RANK_CHANGED(
        code = "GAME_FRIEND_RANK_CHANGED",
        title = "친구 랭킹 변동 📈",
        message = "{nickname_iga} 게임 친구 랭킹이 {beforeRank}위 → {afterRank}위로 변했어!",
        itemType = PushItemType.GAME,
        group = PushTemplateGroup.GAME,
    ),

    /**
     * 게임에서 특정 친구를 추월했을 때 친구에게 알림
     */
    GAME_OVERTAKE_FRIEND(
        code = "GAME_OVERTAKE_FRIEND",
        title = "랭킹 추월 알림 🏃",
        message = "{nickname_iga} {gameName}에서 너를 추월했어! 📉",
        itemType = PushItemType.GAME,
        group = PushTemplateGroup.GAME,
    ),


    /**
     * 초대코드로 가입한 경우 (초대자에게 알림)
     */
    INVITE_CODE_JOINED(
        code = "INVITE_CODE_JOINED",
        title = "초대코드 가입 🎉",
        message = "{nickname_iga} 내 초대코드로 가입했어!",
        itemType = PushItemType.ALL,
        group = PushTemplateGroup.ETC,
    ),

    /**
     * 보상 리그 시작 알림
     * - (시즌, 리그, 게임) 점수 등록자 수가 10명 이상이 되는 순간 1회 발송
     */
    REWARD_LEAGUE_START(
        code = "REWARD_LEAGUE_START",
        title = "보상 리그 시작 알림 🏆",
        message = "지금 보상 리그가 시작됐어. 친구들 보다 먼저 달리고 보상 챙기자!",
        itemType = PushItemType.GAME,
        group = PushTemplateGroup.GAME,
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

