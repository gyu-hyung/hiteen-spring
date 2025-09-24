package kr.jiasoft.hiteen.feature.level.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Schema(description = "사용자 경험치 이력 엔티티")
@Table("user_exp_history")
data class UserExpHistoryEntity(

    @field:Schema(description = "이력 ID", example = "1")
    @Id
    val id: Long = 0,

    @field:Schema(description = "사용자 ID", example = "123")
    val userId: Long,

    @field:Schema(description = "타겟 ID (친구ID, 게시글ID, 댓글ID 등)", example = "123")
    val targetId: Long,

    @field:Schema(description = "액션 코드", example = "POST_CREATE")
    val actionCode: String,

    @field:Schema(description = "획득 경험치", example = "10")
    val points: Int,

    @field:Schema(description = "획득 사유", example = "게시글 작성")
    val reason: String? = null,

    @field:Schema(description = "이력 생성 시각", example = "2025-09-23T13:20:30+09:00")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
