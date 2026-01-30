package kr.jiasoft.hiteen.feature.board.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Table("boards")
data class BoardEntity (
    @Id
    val id: Long = 0,
    val uid: UUID = UUID.randomUUID(),
    val category: String,
    val subject: String?,
    val content: String?,
    val link: String? = null,
    val ip: String? = null,
    val hits: Int = 0,
    val assetUid: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val reportCount: Int = 0,
    val status: String,
    val address: String? = null,
    val detailAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val createdId: Long,
    val createdAt: OffsetDateTime,
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null,
)