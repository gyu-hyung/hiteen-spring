package kr.jiasoft.hiteen.feature.user.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID


@Table("user_photos")
data class UserPhotosEntity (

    @param:Schema(description = "사용자 사진 ID", example = "1")
    @Id
    val id: Long = 0,

    @param:Schema(description = "사용자 ID", example = "1")
    @JsonIgnore
    val userId: Long,

    @param:Schema(description = "사용자 사진 UUID", example = "c264013d-bb1d-4d66-8d34-10962c022056")
    val uid: UUID,
)