package kr.jiasoft.hiteen.feature.play.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "랭킹 정보 view DTO")
data class RankingView(

    @field:Schema(description = "ID", example = "1")
    val id: Int,

    @field:Schema(description = "랭킹 순위", example = "1")
    val rank: Int,

    @field:Schema(description = "유저 ID", example = "1")
    val userId: Long,

    @field:Schema(description = "유저 uid", example = "1")
    val userUid: UUID? = null,

    @field:Schema(description = "닉네임", example = "nickname")
    val nickname: String,

    @field:Schema(description = "학년", example = "3")
    val grade: String,

    @field:Schema(description = "학교 type", example = "3")
    val type: String,

    @field:Schema(description = "학교 이름", example = "홍성여자중학교부설방송통신중학교")
    val schoolName: String,

//    @field:Schema(description = "잘린 학교 이름", example = "홍성여자중학교부설방송통신중학교")
//    val modifiedSchoolName: String? = null,

    @field:Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    val assetUid: String?,

    @field:Schema(description = "점수", example = "63.12 -> 01:03:12")
    val score: BigDecimal,

    @field:Schema(description = "시도 횟수", example = "2")
    val tryCount: Long? = null,

    @param:Schema(description = "랭킹 생성 시간", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime? = null,

    @param:Schema(description = "랭킹 생성 시간", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "시즌 ID", example = "1")
    val seasonId: Long? = null,

    @field:Schema(description = "리그", example = "BRONZE")
    val league: String? = null,

    @field:Schema(description = "게임 ID", example = "1")
    val gameId: Long? = null,

    @field:Schema(description = "참가자 ID", example = "1")
    val participantId: Long? = null,

)
