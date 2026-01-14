package kr.jiasoft.hiteen.feature.chat.app

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.chat.dto.*
import kr.jiasoft.hiteen.feature.chat.infra.ChatUserRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.time.OffsetDateTime
import java.util.*

@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequestMapping("/api/chats")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class ChatController(
    private val service: ChatService,
    private val chatHub: ChatHub,
    private val mapper: ObjectMapper,
    private val chatUserRepository: ChatUserRepository
) {

    @Operation(summary = "내 채팅방 목록 조회", description = "내가 속한 채팅방 목록을 최근순으로 조회합니다.")
    @GetMapping
    suspend fun listRooms(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "조회 개수 (기본 20, 최대 100)") @RequestParam(defaultValue = "20") limit: Int,
        @Parameter(description = "조회 오프셋 (기본 0)") @RequestParam(defaultValue = "0") offset: Int,
    ) = ResponseEntity.ok(
        ApiResult.success(service.listRoomsSnapshot(user.id, limit.coerceIn(1, 100), offset.coerceAtLeast(0)))
    )


    @Operation(summary = "1:1 채팅방 생성", description = "상대방 UID를 지정하여 1:1 채팅방을 생성합니다. 이미 존재하면 기존 방을 반환합니다.")
    @PostMapping("/direct")
    suspend fun createDirectRoom(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "1:1 채팅방 생성 요청 DTO") peerUid: UUID,
    ) = ResponseEntity.ok(ApiResult.success(service.createDirectRoom(user.id, peerUid)))


    @Operation(summary = "그룹 채팅방 생성(JSON, 하위호환)", description = "기존 JSON 기반 생성 API(파일 미지원).")
    @PostMapping("/rooms")
    suspend fun createRoom(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody req: CreateRoomRequest,
    ) : ResponseEntity<ApiResult<Any>> {
        val roomUid = service.createRoom(user.id, req)
        return ResponseEntity.ok(ApiResult.success(roomUid))
    }


    @Operation(summary = "그룹 채팅방 생성", description = "여러 명을 지정하여 단체 채팅방을 생성합니다. (썸네일 파일 업로드 지원, 1개만 가능)")
    @PostMapping("/rooms-with-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun createRoomJson(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "그룹 채팅방 생성 요청 DTO") req: CreateRoomRequest,
        @Parameter(description = "첨부 파일(대표 썸네일, 1개)") @RequestPart(name = "file", required = false) file: FilePart?,
    ) : ResponseEntity<ApiResult<Any>> {
        val roomUid = service.createRoom(user.id, req, file)
        return ResponseEntity.ok(ApiResult.success(roomUid))
    }


    @Operation(summary = "채팅방 상세 조회", description = "특정 채팅방 정보를 UID로 조회합니다. (방 정보 + 참여 멤버)")
    @GetMapping("/{roomUid}")
    suspend fun getRoom(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID
    ) = ResponseEntity.ok(ApiResult.success(service.getRoomByUid(roomUid)))


    @Operation(summary = "채팅방 멤버 초대", description = "채팅방에 멤버를 초대합니다. invite_mode=OWNER이면 방장만, ALL_MEMBERS이면 참여 멤버 누구나 초대 가능")
    @PostMapping("/{roomUid}/invite")
    suspend fun inviteMembers(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "초대 요청 DTO") @RequestBody req: ChatRoomInviteRequest,
    ) = ResponseEntity.ok(ApiResult.success(service.inviteMembers(roomUid, user.id, req.peerUids)))


    //TODO : 호출 시 읽음처리
    @Operation(
        summary = "메시지 목록 조회",
        description = "특정 채팅방의 메시지를 커서 기반 페이징으로 조회합니다. cursor=이전 마지막 메시지 createdAt"
    )
    @GetMapping("/{roomUid}/messages")
    suspend fun listMessages(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID,
        @Parameter(description = "커서(이전 마지막 메시지의 createdAt)") @RequestParam(required = false) cursor: OffsetDateTime?,
        @Parameter(description = "조회 개수 (기본 30, 최대 100)") @RequestParam(defaultValue = "30") size: Int,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = ResponseEntity.ok(ApiResult.success(service.pageMessages(roomUid, cursor, size.coerceIn(1, 100), user.id)))


    //TODO 전송한 메세지 반환
    @Operation(summary = "메시지 전송", description = "특정 채팅방에 메시지를 전송합니다.")
    @PostMapping("/{roomUid}/messages")
    suspend fun sendMessage(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "메시지 전송 요청 DTO") req: SendMessageRequest,
        @Parameter(description = "첨부 파일들") @RequestPart(name = "files", required = false) filesFlux: Flux<FilePart>?,
    ) : ResponseEntity<ApiResult<Any>> {

        val files: List<FilePart> = filesFlux?.collectList()?.awaitSingle() ?: emptyList()

        //메세지 송신
        val msgRes = service.sendMessage(roomUid, user, req, files)

        //message broadcast
        val payload = mapper.writeValueAsString(
            mapOf(
                "type" to "message",
                "data" to msgRes
            )
        )
        chatHub.publish(roomUid, payload)

        chatUserRepository.listActiveUserUidsByUid(roomUid).collect { userUid ->
            chatHub.publishUserNotify(userUid, payload)
        }
        return ResponseEntity.ok(ApiResult.success(msgRes))
    }


    @Operation(summary = "메시지 읽음 처리", description = "특정 메시지를 읽음 처리합니다.")
    @PostMapping("/{roomUid}/messages/{messageUid}/read")
    suspend fun markRead(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID,
        @Parameter(description = "메시지 UID") @PathVariable messageUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = ResponseEntity.ok(ApiResult.success(service.markRead(roomUid, user, messageUid)))


    @Operation(summary = "알림 설정 변경", description = "채팅방의 알림 설정을 ON/OFF 합니다.")
    @PostMapping("/{roomUid}/push")
    suspend fun togglePush(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "알림 활성화 여부") enabled: Boolean
    ) = ResponseEntity.ok(ApiResult.success(service.togglePush(roomUid, user.id, enabled)))


    @Operation(summary = "채팅방 나가기", description = "특정 채팅방에서 나갑니다.")
    @DeleteMapping("/{roomUid}")
    suspend fun leaveRoom(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = ResponseEntity.ok(ApiResult.success(service.leaveRoom(roomUid, user.id)))


    @Operation(summary = "채팅방 수정", description = "채팅방 이름/초대권한/썸네일을 수정합니다. 썸네일 파일은 1개만 업로드 가능합니다.")
    @PostMapping("/{roomUid}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun updateRoom(
        @Parameter(description = "채팅방 UID") @PathVariable roomUid: UUID,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "채팅방 수정 요청 DTO") req: UpdateRoomRequest,
        @Parameter(description = "첨부 파일(대표 썸네일, 1개)") @RequestPart(name = "file", required = false) file: FilePart?,
    ) : ResponseEntity<ApiResult<Unit>> {
        service.updateRoom(roomUid, user.id, req, file)
        return ResponseEntity.ok(ApiResult.success(Unit))
    }

}
