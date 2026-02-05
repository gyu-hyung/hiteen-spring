package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminChatMessageResponse
import kr.jiasoft.hiteen.admin.dto.AdminChatRoomDetailResponse
import kr.jiasoft.hiteen.admin.infra.AdminChatRoomRepository
import kr.jiasoft.hiteen.admin.infra.AdminChatUserRepository
import kr.jiasoft.hiteen.admin.services.AdminChatService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.success
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import kotlin.collections.filter

@Tag(name = "AdminChat", description = "관리자 > 채팅내역")
@RestController
@RequestMapping("/api/admin/chat")
class AdminChatController(
    private val chatService: AdminChatService,
    private val roomRepository: AdminChatRoomRepository,
    private val chatUserRepository: AdminChatUserRepository,
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

        val data = chatService.listMessages(status, startDate, endDate, searchType, search, page, perPage)
        return success(data)
    }

    // 채팅방 갯수
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
    @GetMapping("/room")
    suspend fun roomDetail(
        @RequestParam roomId: Long,
    ): ResponseEntity<ApiResult<AdminChatRoomDetailResponse>> {
        val room = roomRepository.detailById(roomId)
        val users = chatUserRepository.usersById(roomId).toList()

        val activeUsers = users.filter {
            it.isLeaved == "N" && it.userDeleted == "N"
        }

        if (room.roomName.isNullOrBlank()) {
            val names = activeUsers.map { it.userName }
            room.roomName =
                if (names.size >= 4) {
                    val firstThree = names.take(3).joinToString(",")
                    val remain = names.size - 3
                    "$firstThree 외 ${remain}명"
                } else {
                    names.joinToString(",")
                }
        }

        val data = AdminChatRoomDetailResponse(room, users)

        return success(data)
    }

    // 채팅방: 채팅방 정보, 메시지 목록, 채팅회원 목록
    @GetMapping("/room/messages")
    suspend fun listRoomMessages(
        @RequestParam roomId: Long,
        @RequestParam status: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam perPage: Int = 20,
    ): ResponseEntity<ApiResult<ApiPage<AdminChatMessageResponse>>> {
        val data = chatService.roomMessages(roomId, status, search, page, perPage)

        return success(data)
    }
}
