package kr.jiasoft.hiteen.feature.pin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "pin")
data class PinEntity(
    @Id
    val id: Long? = null,
    val userId: Long,
    val zipcode: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val description: String? = null,
    val visibility: String? = null,
    val createdId: Long? = null,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null
)

