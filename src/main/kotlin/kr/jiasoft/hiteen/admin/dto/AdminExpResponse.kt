package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminExpResponse(
    val id: Long? = null,

    val userId: Long? = null,
    var userUid: String? = null,
    val userName: String? = null,
    val userPhone: String? = null,

    val targetId: Long? = null,
    val actionCode: String,
    val points: Int,
    val reason: String? = null,

    val statusName: String? = if (points < 0) "차감" else "적립",

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: LocalDateTime? = null,
    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val createdDate: String? = createdAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),
)