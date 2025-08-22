package kr.jiasoft.hiteen.feature.user

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.jiasoft.hiteen.feature.user.dto.CustomUserDetails
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime


@Table(name = "users")
data class UserEntity(
    @Id
    @JsonIgnore
    val id: Long? = null,
    val uid: String,
    val username: String,
    val email: String? = null,
    val nickname: String? = null,
    @JsonIgnore
    val password: String,
    val role: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null,
)

fun UserEntity.toUserDetails(): UserDetails = CustomUserDetails(this)
fun UserEntity.toResponse(): UserResponse = UserResponse(
    id = this.id,
    username = this.username,
    email = this.email,
    nickname = this.nickname,
    role = this.role,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
)
