package kr.jiasoft.hiteen.feature.chat.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.chat.dto.CreateDirectRoomRequest
import kr.jiasoft.hiteen.feature.chat.dto.CreateRoomRequest
import kr.jiasoft.hiteen.feature.chat.dto.SendMessageRequest
import kr.jiasoft.hiteen.feature.chat.dto.TogglePushRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.*

@RestController
@RequestMapping("/api/chats")
class ChatController(
    private val service: ChatService,
) {

    /** 내가 속한 채팅방 목록 (최근순)
     * TODO : 각 채팅방 썸네일
     * */
    @GetMapping
    suspend fun listRooms(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "0") offset: Int,
    ) = ResponseEntity.ok(ApiResult.success(service.listRoomsSnapshot(user.id!!, limit.coerceIn(1, 100), offset.coerceAtLeast(0))))


    /** 1:1 방 만들기(이미 있으면 재사용) */
    @PostMapping
    suspend fun createDirectRoom(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: CreateDirectRoomRequest
    ) = ResponseEntity.ok(ApiResult.success(service.createDirectRoom(user.id!!, req.peerUid)))


    /** 단톡 생성 */
    @PostMapping("/rooms")
    suspend fun createRoom(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: CreateRoomRequest
    ) = ResponseEntity.ok(ApiResult.success(service.createRoom(user.id!!, req.peerUids, req.reuseExactMembers)))


    /** 방 상세 가져오기(필요시) */
    @GetMapping("/{roomUid}")
    suspend fun getRoom(@PathVariable roomUid: UUID) = ResponseEntity.ok(ApiResult.success(service.getRoomByUid(roomUid)))


    /** 메시지 페이징 (무한스크롤, cursor=이전 마지막 메세지의 createdAt)
     * TODO : 호출 시 읽음처리 */
    @GetMapping("/{roomUid}/messages")
    suspend fun listMessages(
        @PathVariable roomUid: UUID,
        @RequestParam(required = false) cursor: OffsetDateTime?,
        @RequestParam(defaultValue = "30") size: Int,
    ) = ResponseEntity.ok(ApiResult.success(service.pageMessages(roomUid, cursor, size.coerceIn(1, 100))))


    /** 메세지 전송 */
    @PostMapping("/{roomUid}/messages")
    suspend fun sendMessage(
        @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: SendMessageRequest,
    ) = ResponseEntity.ok(ApiResult.success(service.sendMessage(roomUid, user, req)))


    /** 메세지 읽음 처리 */
    @PostMapping("/{roomUid}/messages/{messageUid}/read")
    suspend fun markRead(
        @PathVariable roomUid: UUID,
        @PathVariable messageUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = ResponseEntity.ok(ApiResult.success(service.markRead(roomUid, user, messageUid)))


    /** 알림 on/off */
    @PostMapping("/{roomUid}/push")
    suspend fun togglePush(
        @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
         req: TogglePushRequest
    ) = ResponseEntity.ok(ApiResult.success(service.togglePush(roomUid, user.id!!, req.enabled)))


    /** 방 나가기 */
    @PostMapping("/{roomUid}/leave")
    suspend fun leaveRoom(
        @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = ResponseEntity.ok(ApiResult.success(service.leaveRoom(roomUid, user.id!!)))

}
