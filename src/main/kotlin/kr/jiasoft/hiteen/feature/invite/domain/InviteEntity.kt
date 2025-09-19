package kr.jiasoft.hiteen.feature.invite.domain

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("invites")
data class InviteEntity (

    @Id
    val id: Long = 0,
    val type: String = "Register",
    val userId: Long,
    val phone: String,
    val code: String,
    val status: Int,
    val joinId: Long,
    val joinPoint: Int,

    @param:Schema(description = "가입 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val joinDate: OffsetDateTime,

    @param:Schema(description = "탈퇴 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val leaveDate: OffsetDateTime? = null,
)