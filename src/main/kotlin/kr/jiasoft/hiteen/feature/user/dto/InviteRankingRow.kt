package kr.jiasoft.hiteen.feature.user.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 초대 랭킹 조회용 Projection 인터페이스
 */
data class InviteRankingRow (
    @get:Schema(description = "사용자 ID")
    val id: Long,

    @get:Schema(description = "닉네임")
    val nickname: String?,

    @get:Schema(description = "초대한 친구 수")
    val inviteJoins: Long,

    @get:Schema(description = "프로필 이미지 UID")
    val assetUid: String?,

    @get:Schema(description = "학년")
    val grade: String?,

    @get:Schema(description = "학교 type")
    val type: String?,

    @get:Schema(description = "학교 이름")
    val schoolName: String?,
)

