package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime
import java.util.UUID

data class AdminPinResponse(
    val id: Long,
    val userId: Long,
    val userUid: UUID,
    val nickname: String,

    val zipcode: String?,
    val lat: Double?,
    val lng: Double?,
    val description: String?,
    val visibility: String,


    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime,

    val status: String,
)
