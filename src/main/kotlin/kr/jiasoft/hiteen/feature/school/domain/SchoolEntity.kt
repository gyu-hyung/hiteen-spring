package kr.jiasoft.hiteen.feature.school.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Table("schools")
data class SchoolEntity(
    @Id
    val id: Long? = null,
    val sido: String?,
    val sidoName: String?,
    val code: String,
    val name: String,
    val type: Int = 9,
    val typeName: String?,
    val zipcode: String?,
    val address: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val coords: String? = null,
    val createdId: Long? = null,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val updatedId: Long? = null,
    val updatedAt: LocalDateTime? = LocalDateTime.now(),
    val deletedId: Long? = null,
    val deletedAt: LocalDateTime? = null,
    @Column("found_date")
    val foundDate: LocalDate? = null
)

