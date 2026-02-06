package kr.jiasoft.hiteen.admin.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminCashResponse(
    val id: Long? = null,
    val userId: Long? = null,
    val cashableType: String? = null,
    val cashableId: Long? = null,
    val type: String,
    val amount: Int,
    val memo: String? = null,

    var userUid: String? = null,
    val nickname: String? = null,
    val phone: String? = null,

    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: LocalDateTime? = null,
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val deletedAt: LocalDateTime? = null,

    @get:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yy.MM.dd HH:mm")
    val createdDate: String? = createdAt?.format(DateTimeFormatter.ofPattern("yy.MM.dd HH:mm")),

    @param:Schema(description = "적립/차감 구분명", example = "적립")
    val typeName: String? = if (type === "DEBIT") "차감" else "적립",
)

