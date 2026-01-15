package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class AdminSchoolClassesResponse(
    val id: Long = 0,
    val code: String,
    val year: Int = 0,
    val schoolId: Long,
    val schoolName: String,
    val schoolType: Int,
    val className: String,
    val major: String? = null,
    val grade: String,
    val classNo: String,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val createdAt: LocalDate? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val updatedAt: LocalDate? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val deletedAt: LocalDate? = null,

    @JsonIgnore @param:Schema(hidden = true) val createdId: Long? = null,
    @JsonIgnore @param:Schema(hidden = true) val updatedId: Long? = null,
    @JsonIgnore @param:Schema(hidden = true) val deletedId: Long? = null,
)