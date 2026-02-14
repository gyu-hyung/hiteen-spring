package kr.jiasoft.hiteen.feature.invite.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(name = "InviteDeferredIssueResponse")
data class InviteDeferredIssueResponse(
    val token: String,
)

@Schema(name = "InviteDeferredResolveResponse")
data class InviteDeferredResolveResponse(
    val code: String,
)

@Schema(name = "InviteRankingResponse", description = "초대 랭킹 응답")
data class InviteRankingResponse(

    @field:Schema(description = "순위")
    val rank: Int,
    @JsonIgnore
    @field:Schema(description = "사용자 ID")
    val userId: Long,
    @field:Schema(description = "닉네임")
    val nickname: String,
    @field:Schema(description = "초대한 친구 수")
    val inviteCount: Long,
    @field:Schema(description = "프로필 이미지 UID")
    val assetUid: String?,
    @field:Schema(description = "학년", example = "3")
    val grade: String? = null,
    @field:Schema(description = "학교 type", example = "middle")
    val type: String? = null,
    @field:Schema(description = "학교 이름", example = "홍성여자중학교")
    val schoolName: String? = null,
    @field:Schema(description = "잘린 학교 이름", example = "홍성여자중")
    val modifiedSchoolName: String? = null,
)

@Schema(name = "InviteStatsResponse", description = "기간별 회원가입 통계 응답")
data class InviteStatsResponse(
    @field:Schema(description = "시작일")
    val startDate: OffsetDateTime,
    @field:Schema(description = "종료일")
    val endDate: OffsetDateTime,
    @field:Schema(description = "가입자 수")
    val count: Long,
    @field:Schema(description = "그래프 최소값")
    val min: Long = 0,
    @field:Schema(description = "그래프 최대값")
    val max: Long = 10000,
)
