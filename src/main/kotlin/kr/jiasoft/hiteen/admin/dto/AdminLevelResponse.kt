package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class AdminLevelResponse(
    val id: Long? = null,
    val tierCode: String,
    val tierNameKr: String,
    val divisionNo: Int = 0,
    val level: Int = 0,
    val rankOrder: Int = 0,
    val status: String = "ACTIVE",
    val minPoints: Int,
    val maxPoints: Int,
    val uid: UUID,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: OffsetDateTime? = null,

    @param:Schema(description = "회원수", example = "12")
    val memberCount: Long = 0,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val createdDate: String? = createdAt?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val updatedDate: String? = updatedAt?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val deletedDate: String? = deletedAt?.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
)