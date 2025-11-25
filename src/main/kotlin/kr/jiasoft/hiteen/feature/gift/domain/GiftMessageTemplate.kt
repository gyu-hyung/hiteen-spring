package kr.jiasoft.hiteen.feature.gift.domain

enum class GiftMessageTemplate(
    val defaultMemo: String?,
    val defaultGoodsName: String?,
    val defaultMmsTitle: String,
    val defaultMmsMsg: String?,
    val dynamic: Boolean = false
) {

    Join(
        defaultMemo = "가입 축하 리워드",
        defaultGoodsName = "🎉 하이틴 가입 축하 선물",
        defaultMmsTitle = "[하이틴] 가입 기념 선물 도착!",
        defaultMmsMsg = "가입을 축하해요! 하이틴과 함께 좋은 추억 만들어봐요 🎁",
    ),

    Challenge(
        defaultMemo = null,
        defaultGoodsName = null,
        defaultMmsTitle = "[하이틴] 챌린지 보상 도착!",
        defaultMmsMsg = null,
        dynamic = true // 🔥 Challenge 는 동적 정책
    ),

    Admin(
        defaultMemo = "관리자 지급",
        defaultGoodsName = "🎁 관리자 특별 지급",
        defaultMmsTitle = "[하이틴] 특별 리워드 안내",
        defaultMmsMsg = "관리자가 보내는 특별한 선물입니다 🎈",
    ),

    Event(
        defaultMemo = "이벤트 참여 리워드",
        defaultGoodsName = "🎁 이벤트 참여 보상",
        defaultMmsTitle = "[하이틴] 이벤트 당첨 안내!",
        defaultMmsMsg = "축하합니다! 이벤트 리워드가 도착했습니다! 🎉",
    );
}


fun GiftCategory.toTemplate(): GiftMessageTemplate {
    return GiftMessageTemplate.valueOf(this.name)
}
