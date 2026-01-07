package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode
import java.time.LocalDate
import java.time.OffsetDateTime

data class AdminFriendResponse (
    val id: Long,
    val userId: Long,
    val friendId: Long,
    val status: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val statusAt: OffsetDateTime? = null,
    val userLocationMode: LocationMode,
    val friendLocationMode: LocationMode,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,

    // 🔹 추가된 컬럼들

    val nickname: String,
    val phone: String,
    val gender: String,
    val birthday: LocalDate? = null,
    val schoolName: String,
)