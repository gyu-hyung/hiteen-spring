package kr.jiasoft.hiteen.feature.timetable.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("time_image")
data class TimeTableImageEntity(
    @Id
    val id: Long = 0,
    val classId: Long,
    val semester: Int = 1,
    val userId: Long?,
    val image: String?,
    val reportCount: Int = 0,
    val status: Int = 1,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null
)
