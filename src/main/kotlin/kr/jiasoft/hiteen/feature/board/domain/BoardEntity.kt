package kr.jiasoft.hiteen.feature.board.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Table("boards")
data class BoardEntity (
    @Id
    val id: Long? = null,
    val uid: UUID = UUID.randomUUID(),
    val category: String? = null,
    val subject: String? = null,
    val content: String? = null,
    val link: String? = null,
    val ip: String? = null,
    val hits: Int = 0,
    val assetUid: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val reportCount: Int = 0,
    val status: String? = null,
    val address: String? = null,
    val detailAddress: String? = null,
    val createdId: Long ? = null,
    val createdAt: OffsetDateTime? = null,
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null,
)