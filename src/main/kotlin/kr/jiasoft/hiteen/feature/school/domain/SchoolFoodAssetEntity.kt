package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

//TODO
@Table("tb_school_food_images")
data class SchoolFoodAssetEntity(
    @Id
    val id: Long? = null,
    val schoolId: Long,
    val year: Int,
    val month: Int,
    val userId: Long,
    val image: UUID,
    val reportCount: Int,
    val status: String = "Y",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null
)