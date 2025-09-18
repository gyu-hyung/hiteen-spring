package kr.jiasoft.hiteen.feature.pin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "허가된 친구")
data class AllowedFriend(

    @param:Schema(description = "유저 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userUid: String,

    @param:Schema(description = "닉네임", example = "닉네임1")
    val nickname: String? = null,
)