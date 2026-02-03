package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.admin.dto.AdminChatMessageResponse
import kr.jiasoft.hiteen.admin.infra.AdminChatRoomRepository
import kr.jiasoft.hiteen.admin.services.AdminChatService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Tag(name = "AdminChat", description = "관리자 > 채팅내역")
@RestController
@RequestMapping("/api/admin/chat")
class AdminChatController(
    private val chatService: AdminChatService,
    private val roomRepository: AdminChatRoomRepository,
) {
    // 채팅 메시지 목록
    @GetMapping("/messages")
    suspend fun listMessages(
        @RequestParam status: String? = null,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam perPage: Int = 10,
    ): ResponseEntity<ApiResult<ApiPage<AdminChatMessageResponse>>> {
        val startDate = startDate?.atStartOfDay()
        val endDate = endDate?.plusDays(1)?.atStartOfDay()

        val data = chatService.listMessages(status, startDate, endDate, searchType, search, page, perPage,)
        return success(data)
    }

    // 채팅방수
    @GetMapping("/rooms/total")
    suspend fun totalChatRooms(): ResponseEntity<ApiResult<Map<String, Long>>> {
        val activeCount = roomRepository.countByDeletedAtIsNull()
        val deletedCount = roomRepository.countByDeletedAtIsNotNull()
        val data = mapOf(
            "activeCount" to activeCount,
            "deletedCount" to deletedCount
        )

        return success(data)
    }


    // 채팅방 정보
    /*
    @GetMapping("/room")
    suspend fun getRoom(
        @RequestParam roomUid: UUID,
    ): ResponseEntity<ApiResult<AdminChatRoomDetailResponse>> {
        val data = chatService.getRoomByUid(roomUid)
        return success(data)
    }

    // 채팅방 메시지 목록
    @GetMapping("/room/messages")
    suspend fun listRoomMessages(
        @RequestParam roomUid: UUID,
        @RequestParam cursor: OffsetDateTime?,
        @RequestParam perPage: Int = 20,
    ): ResponseEntity<ApiResult<ApiPage<AdminChatMessageResponse>>> {
        val data = chatService.listMessages(roomUid, cursor, perPage.coerceIn(1, 100))
        return success(data)
    }
    */
}
