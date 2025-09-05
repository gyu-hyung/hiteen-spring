package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

//TODO
@Table("tb_classes")
data class ClassEntity(
    @Id
    val id: Long? = null,
    val code: String,           // UUID
    val year: Int,              // 학년도
    val schoolId: Long,         // 학교번호 (FK)
    val schoolName: String,
    val schoolType: Int,        // 학교구분
    val className: String,      // 학급명
    val major: String?,
    val grade: String?,         // 학년
    val classNum: String?,      // 반
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = LocalDateTime.now(),
    val deletedAt: LocalDateTime? = null
)
