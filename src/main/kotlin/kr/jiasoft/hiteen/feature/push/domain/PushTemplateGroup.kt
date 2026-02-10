package kr.jiasoft.hiteen.feature.push.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "푸시 템플릿 그룹")
enum class PushTemplateGroup(
    @field:Schema(description = "그룹 코드")
    val code: String,

    @field:Schema(description = "그룹명")
    val title: String,
) {
    FRIEND(code = "FRIEND", title = "친구"),
    FOLLOW(code = "FOLLOW", title = "팔로우"),
    GAME(code = "GAME", title = "게임"),
    TEEN_STORY(code = "TEEN_STORY", title = "틴스토리"),
    TEEN_VOTE(code = "TEEN_VOTE", title = "틴투표"),
    COMMENT(code = "COMMENT", title = "댓글"),
    GIFT_SHOP(code = "GIFT_SHOP", title = "선물샵"),
    PIN(code = "PIN", title = "핀"),
    ETC(code = "ETC", title = "기타"),
    MARKETING(code = "MARKETING", title = "마케팅"),
}
