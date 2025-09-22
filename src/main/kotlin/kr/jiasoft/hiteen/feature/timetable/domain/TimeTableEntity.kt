package kr.jiasoft.hiteen.feature.timetable.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Table("time_table")
data class TimeTableEntity (
    @Id
    val id: Long = 0,
    val classId: Long,
    val year: Int,
    val semester: Int,
    val timeDate: LocalDate,
    val period: Int,
    val subject: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null

)