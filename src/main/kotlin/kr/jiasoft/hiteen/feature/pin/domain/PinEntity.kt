package kr.jiasoft.hiteen.feature.pin.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "pin")
data class PinEntity(
    @Id
    val id: Long = 0,
    val userId: Long,
    val zipcode: String?,
    val lat: Double,
    val lng: Double,
    val description: String,
    val visibility: String,
    @JsonIgnore
    val createdId: Long,
    val createdAt: OffsetDateTime,
    @JsonIgnore
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null,
)

