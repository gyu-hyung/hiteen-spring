package kr.jiasoft.hiteen.feature.user.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID


@Table("user_photos")
data class UserPhotosEntity (
    @Id
    val id: Long = 0,
    @JsonIgnore
    val userId: Long,
    val uid: UUID,
)