package kr.jiasoft.hiteen.feature.board.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "이벤트 배너 응답")
data class EventBannerDto(
    @field:Schema(description = "배너 ID", example = "1")
    val id: Long,

    @field:Schema(description = "이벤트(board) ID", example = "10")
    val boardId: Long,

    @field:Schema(description = "배너 이미지 UID(asset uid)", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID,

    @field:Schema(description = "정렬 순서", example = "1")
    val seq: Int,
)

