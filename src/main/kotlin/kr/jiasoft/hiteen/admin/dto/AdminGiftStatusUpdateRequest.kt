package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "관리자 선물 상태 변경 요청")
data class AdminGiftStatusUpdateRequest(
    @field:Schema(description = "선물 UID")
    val giftUid: UUID,

    @field:Schema(description = "변경할 상태 코드 (4: 배송요청, 5: 배송완료, 6: 지급요청, 7: 지급완료)")
    val status: Int,

    @field:Schema(description = "메모 (선택)")
    val memo: String? = null,
)

