package kr.jiasoft.hiteen.feature.user.dto

import java.time.LocalDateTime

data class UserResponse(
    val id: Long?,
    val username: String,
    val email: String?,
    val nickname: String?,
    val role: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
)
