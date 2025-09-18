package kr.jiasoft.hiteen.feature.relationship.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "친구 검색 결과")
data class RelationshipSearchItem(

    @param:Schema(description = "사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: String,

    @param:Schema(description = "사용자 ID", example = "user123")
    val username: String,

    @param:Schema(description = "사용자 별명", example = "별명")
    val nickname: String?,

    @param:Schema(description = "관계 상태", example = "ACCEPTED")
    val relation: String? = null // null|PENDING_OUT|PENDING_IN|ACCEPTED|BLOCKED|SELF
)