package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Schema(description = "관리자 > 요청/승인 > 친구")
data class AdminAcceptFriendResponse (
    val id: Long,

    // 요청회원
    val userId: Long,
    val userName: String?,
    val userPhone: String?,

    // 수신회원
    val targetId: Long,
    val targetName: String?,
    val targetPhone: String?,

    val status: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val statusAt: OffsetDateTime? = null,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val statusDate: String? = statusAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),

    // 위치모드
    val userLocationMode: LocationMode,
    val friendLocationMode: LocationMode,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val createdDate: String? = createdAt.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),
)

@Schema(description = "관리자 > 요청/승인 > 팔로우")
data class AdminAcceptFollowResponse(
    val id: Long,

    // 요청회원
    val userId: Long,
    val userName: String?,
    val userPhone: String?,

    // 수신회원
    val targetId: Long,
    val targetName: String?,
    val targetPhone: String?,

    val status: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val statusAt: OffsetDateTime?,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val statusDate: String? = statusAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val createdDate: String? = createdAt.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),
)
