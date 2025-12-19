package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminSchoolResponse(
    val id: Long? = null,
    val sido: String? = null,
    val sidoName: String? = null,
    val code: String,
    val name: String,
    val type: Int? = null,
    val typeName: String? = null,
    val zipcode: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val coords: String? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val foundDate: LocalDate? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: LocalDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: LocalDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: LocalDateTime? = null,

    @JsonIgnore @param:Schema(hidden = true) val createdId: Long? = null,
    @JsonIgnore @param:Schema(hidden = true) val updatedId: Long? = null,
    @JsonIgnore @param:Schema(hidden = true) val deletedId: Long? = null,

    @param:Schema(description = "회원수", example = "12")
    val memberCount: Long = 0,

    @param:Schema(description = "학급수", example = "12")
    val classCount: Long = 0,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val updatedDate: String? = updatedAt?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
)