package kr.jiasoft.hiteen.feature.notification.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "푸시 템플릿 그룹 응답")
data class PushTemplateGroupResponse(
    @field:Schema(description = "그룹 코드", example = "FRIEND")
    val groupCode: String,

    @field:Schema(description = "그룹명", example = "친구")
    val groupTitle: String,

    @field:Schema(description = "그룹에 속한 템플릿 목록")
    val templates: List<PushTemplateInfo>,
)

@Schema(description = "푸시 템플릿 정보")
data class PushTemplateInfo(
    @field:Schema(description = "템플릿 코드", example = "FRIEND_REQUEST")
    val code: String,

    @field:Schema(description = "제목")
    val title: String,

    @field:Schema(description = "메시지 템플릿")
    val message: String,
)
