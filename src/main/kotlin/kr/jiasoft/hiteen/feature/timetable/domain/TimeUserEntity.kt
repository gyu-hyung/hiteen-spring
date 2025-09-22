package kr.jiasoft.hiteen.feature.timetable.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("time_user")
data class TimeUserEntity(
    @Id
    val id: Long = 0,
    val classId: Long,
    val userId: Long?,
    val week: Int,
    val period: Int,
    val subject: String?,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null
)