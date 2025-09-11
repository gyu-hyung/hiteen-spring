package kr.jiasoft.hiteen.feature.chat.app

import kr.jiasoft.hiteen.feature.chat.dto.CreateDirectRoomRequest
import kr.jiasoft.hiteen.feature.chat.dto.CreateRoomRequest
import kr.jiasoft.hiteen.feature.chat.dto.SendMessageRequest
import kr.jiasoft.hiteen.feature.chat.dto.TogglePushRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.*

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val service: ChatService,
) {

    /** 내가 속한 채팅방 목록 (최근순) */
    @GetMapping("/rooms")
    suspend fun listRooms(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ) = service.listRoomsSnapshot(user.id!!, limit.coerceIn(1, 100), offset.coerceAtLeast(0))


    /** 1:1 방 만들기(이미 있으면 재사용) */
    @PostMapping("/rooms/direct")
    suspend fun createDirectRoom(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: CreateDirectRoomRequest
    ): Map<String, Any> = mapOf("roomUid" to service.createDirectRoom(user.id!!, req.peerUid))


    /** 단톡 생성 */
    @PostMapping("/rooms")
    suspend fun createRoom(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: CreateRoomRequest
    ): Map<String, Any> =
        mapOf("roomUid" to service.createRoom(user.id!!, req.peerUids, req.reuseExactMembers))


    /** 방 상세 가져오기(필요시) */
    @GetMapping("/rooms/{roomUid}")
    suspend fun getRoom(@PathVariable roomUid: UUID) = service.getRoomByUid(roomUid)


    /** 메시지 페이징 (무한스크롤, cursor=이전 마지막 메세지의 createdAt) */
    @GetMapping("/rooms/{roomUid}/messages")
    suspend fun listMessages(
        @PathVariable roomUid: UUID,
        @RequestParam(required = false) cursor: OffsetDateTime?,
        @RequestParam(defaultValue = "30") size: Int,
    ) = service.pageMessages(roomUid, cursor, size.coerceIn(1, 100))


    /** 메세지 전송 */
    @PostMapping("/rooms/{roomUid}/messages")
    suspend fun sendMessage(
        @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: SendMessageRequest,
    ): Map<String, Any> = mapOf("messageUid" to service.sendMessage(roomUid, user, req))


    /** 메세지 읽음 처리 */
    @PostMapping("/rooms/{roomUid}/messages/{messageUid}/read")
    suspend fun markRead(
        @PathVariable roomUid: UUID,
        @PathVariable messageUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = service.markRead(roomUid, user, messageUid).let { mapOf("ok" to true) }


    /** 알림 on/off */
    @PostMapping("/rooms/{roomUid}/push")
    suspend fun togglePush(
        @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
         req: TogglePushRequest
    ) = service.togglePush(roomUid, user.id!!, req.enabled).let { mapOf("ok" to true) }


    /** 방 나가기 */
    @PostMapping("/rooms/{roomUid}/leave")
    suspend fun leaveRoom(
        @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = service.leaveRoom(roomUid, user.id!!).let { mapOf("ok" to true) }

}
