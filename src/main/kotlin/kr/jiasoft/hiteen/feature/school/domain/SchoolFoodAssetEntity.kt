package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

//TODO
@Table("school_food_image")
data class SchoolFoodAssetEntity(
    @Id
    val id: Long = 0,
    val schoolId: Long,
    val year: Int,
    val month: Int,
    val userId: Long,
    val image: String,
    val reportCount: Int = 0,
    val status: Int = 1,   // 1: 노출, 0: 비노출
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null
)