package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

//TODO
@Table("tb_time_table")
data class TimeTableEntity(
    @Id
    val id: Long? = null,
    val classId: Long,          // 학급번호 (FK)
    val year: Int,
    val semester: Int,
    val timeDate: LocalDateTime,
    val period: Int,            // 교시
    val subject: String?,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null
)