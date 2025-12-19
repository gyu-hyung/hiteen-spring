package kr.jiasoft.hiteen.admin.dto

import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode
import java.time.LocalDate
import java.time.OffsetDateTime

data class AdminFriendResponse (
    val id: Long,
    val userId: Long,
    val friendId: Long,
    val status: String,
    val statusAt: OffsetDateTime? = null,
    val userLocationMode: LocationMode,
    val friendLocationMode: LocationMode,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,

    // 🔹 추가된 컬럼들

    val nickname: String,
    val phone: String,
    val gender: String,
    val birthday: LocalDate? = null,
    val schoolName: String,
)