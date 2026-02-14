package kr.jiasoft.hiteen.feature.block.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사용자 차단 요청")
data class BlockUserRequest(
    @field:Schema(description = "차단할 사용자 UID", required = true)
    val targetUid: String,

    @field:Schema(description = "차단 사유 (선택)")
    val reason: String? = null,
)

@Schema(description = "사용자 차단 해제 요청")
data class UnblockUserRequest(
    @field:Schema(description = "차단 해제할 사용자 UID", required = true)
    val targetUid: String,
)

