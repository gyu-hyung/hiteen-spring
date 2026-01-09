package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.OffsetDateTime

data class AdminPlayResponse (
    val id: Long,
    val uid: String,
    val nickname: String,
    val seasonNo: String,
    val gameName: String,
    val score: String? = null,
    val status: String,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime
)