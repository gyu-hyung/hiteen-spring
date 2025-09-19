package kr.jiasoft.hiteen.feature.attend.domain

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.OffsetDateTime

@Table("attends")
data class AttendEntity(
    @Id
    val id: Long = 0,

    val userId: Long,

    val attendDate: LocalDate,

    val sumDay: Short = 0,

    val point: Int = 0,

    val addPoint: Int = 0,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
