package kr.jiasoft.hiteen.feature.banner.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Table("banners")
data class BannerEntity(
    @Id
    val id: Long = 0,
    val uid: UUID? = UUID.randomUUID(),
    val category: String,
    val title: String? = null,
    val linkType: String? = null,
    val linkUrl: String? = null,
    val bbsCode: String? = null,
    val bbsId: Long? = null,
    val assetUid: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String = "ACTIVE",
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)

