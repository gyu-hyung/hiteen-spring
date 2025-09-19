package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

//TODO
@Table("time_images")
data class TimeTableImageEntity(
    @Id
    val id: Long? = null,
    val classId: Long,
    val userId: Long,
    val week: Int,
    val period: Int,
    val subject: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null
)