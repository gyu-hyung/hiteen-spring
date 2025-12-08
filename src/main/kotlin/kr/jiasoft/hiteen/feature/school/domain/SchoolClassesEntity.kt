package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("school_classes")
data class SchoolClassesEntity(
    @Id
    val id: Long = 0,
    val code: String,
    val year: Int,
    @Column("school_id")
    val schoolId: Long,
    @Column("school_name")
    val schoolName: String,
    @Column("school_type")
    val schoolType: Int,
    @Column("class_name")
    val className: String,
    val major: String?,
    val grade: String,
    @Column("class_no")
    val classNo: String,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val updatedId: Long? = null,
    val updatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null
)
