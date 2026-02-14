package kr.jiasoft.hiteen.feature.block.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "차단한 사용자 정보")
data class BlockedUserResponse(
    @field:Schema(description = "차단 ID")
    val id: Long,

    @field:Schema(description = "차단한 사용자 UID")
    val blockedUserUid: String,

    @field:Schema(description = "차단한 사용자 닉네임")
    val blockedUserNickname: String?,

    @field:Schema(description = "차단한 사용자 프로필 이미지")
    val blockedUserAssetUid: String?,

    @field:Schema(description = "차단 사유")
    val reason: String?,

    @field:Schema(description = "차단 일시")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
)

