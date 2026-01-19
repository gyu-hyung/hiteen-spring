package kr.jiasoft.hiteen.feature.relationship.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.relationship.dto.ContactResponse
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSearchItem
import kr.jiasoft.hiteen.feature.relationship.dto.UpdateLocationModeRequest
import kr.jiasoft.hiteen.feature.relationship.dto.ContactSyncJobCreateResponse
import kr.jiasoft.hiteen.feature.relationship.dto.ContactSyncJobStatusResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.zip.GZIPInputStream

@Tag(name = "Friend", description = "친구 관리 API")
@RestController
@RequestMapping("/api/friends")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class FriendController(
    private val friendService: FriendService,
    private val contactSyncJobService: ContactSyncJobService,
) {

    @Operation(summary = "내 친구 목록 조회", description = "수락된 친구 목록을 조회합니다.")
    @GetMapping
    suspend fun list(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, friendService.listFriends(user)))


    @Operation(summary = "친구 검색", description = "유저 uid/username/nickname/email 로 검색합니다.")
    @GetMapping("/search")
    suspend fun search(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "검색어") @RequestParam q: String,
        @Parameter(description = "검색 결과 제한 (기본 30)") @RequestParam(required = false, defaultValue = "30") limit: Int
    ): ResponseEntity<ApiResult<List<RelationshipSearchItem>>> {
        return ResponseEntity.ok(ApiResult(true, friendService.search(user, q, limit)))
    }


    @Operation(summary = "연락처로 친구 목록 조회 (텍스트)", description = "연락처 문자열을 직접 전달하여 친구 목록을 조회합니다.")
    @PostMapping("/contacts", consumes = [MediaType.TEXT_PLAIN_VALUE])
    suspend fun getContacts(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody rawContacts: String
    ) = ResponseEntity.ok(ApiResult(true, friendService.getContacts(user, rawContacts)))


    @Operation(summary = "연락처로 친구 목록 조회 (파일 업로드)", description = "압축된 연락처 파일(gzip)을 업로드하여 친구 목록을 조회합니다.")
    @PostMapping("/contacts/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadContacts(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "gzip 압축된 연락처 파일") @RequestPart("file") filePart: FilePart
    ): ResponseEntity<ContactResponse> {
        val compressedBytes = filePart.content()
            .asFlow()
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                bytes
            }
            .toList()
            .reduce { acc, bytes -> acc + bytes }

        val rawContacts = GZIPInputStream(compressedBytes.inputStream())
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val response = friendService.getContacts(user, rawContacts)
        return ResponseEntity.ok(response)
    }


    @Operation(summary = "내가 보낸 친구 요청 목록", description = "아직 수락되지 않은 내가 보낸 요청들을 조회합니다.")
    @GetMapping("/requests/outgoing")
    suspend fun outgoing(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, friendService.listOutgoing(user)))


    @Operation(summary = "내가 받은 친구 요청 목록", description = "아직 수락하지 않은 내가 받은 요청들을 조회합니다.")
    @GetMapping("/requests/incoming")
    suspend fun incoming(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, friendService.listIncoming(user)))


    @Operation(summary = "친구 요청 보내기", description = "현재 로그인한 사용자(me)가 특정 사용자(uid)에게 친구 요청을 보냅니다.")
    @PostMapping("/request/{userUid}")
    suspend fun request(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "친구 요청 대상 사용자 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, friendService.request(user, userUid)))


    @Operation(summary = "친구 요청 수락", description = "특정 사용자가 보낸 친구 요청을 수락합니다.")
    @PostMapping("/accept/{userUid}")
    suspend fun accept(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "요청 보낸 사용자 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, friendService.accept(user, userUid)))


    @Operation(summary = "친구 요청 거절", description = "특정 사용자가 보낸 친구 요청을 거절합니다.")
    @DeleteMapping("/reject/{userUid}")
    suspend fun reject(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "요청 보낸 사용자 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, friendService.reject(user, userUid)))


    @Operation(summary = "내가 보낸 친구 요청 취소", description = "내가 보낸 요청을 취소합니다.")
    @DeleteMapping("/cancel/{userUid}")
    suspend fun cancel(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "취소할 요청 대상 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, friendService.cancel(user, userUid)))


    @Operation(summary = "친구 끊기", description = "기존 친구 관계를 해제합니다.")
    @DeleteMapping("/unfriend/{userUid}")
    suspend fun unfriend(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "친구 끊을 대상 UID") @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, friendService.unfriend(user, userUid)))


    @Operation(summary = "친구 위치 모드 변경", description = "친구 위치 모드를 ON/OFF/안개모드 등으로 변경합니다.")
    @PostMapping("/location-mode/{userUid}")
    suspend fun updateLocationMode(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "위치 모드 변경 요청 DTO") req: UpdateLocationModeRequest
    ): ResponseEntity<ApiResult<String>> {
        val friend = friendService.findUserByUid(req.userUid)
            ?: throw IllegalArgumentException("해당 UID의 사용자를 찾을 수 없습니다.")

        friendService.updateLocationMode(user.id, friend.id, req.mode)

        return ResponseEntity.ok(ApiResult.success("위치 모드가 '${req.mode}' 로 변경되었습니다."))
    }


    @Operation(
        summary = "연락처 동기화 Job 생성(비동기)",
        description = "연락처가 많아 동기 응답이 오래 걸릴 때 사용합니다. 즉시 jobId를 반환하고, 결과는 status API로 조회합니다."
    )
    @PostMapping("/contacts/jobs", consumes = [MediaType.TEXT_PLAIN_VALUE])
    suspend fun createContactsJob(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody rawContacts: String,
    ): ResponseEntity<ApiResult<ContactSyncJobCreateResponse>> {
        val jobId = contactSyncJobService.createJob(user.id, rawContacts)
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResult(true, ContactSyncJobCreateResponse(jobId)))
    }


    @Operation(
        summary = "연락처 동기화 Job 상태 조회",
        description = "jobId로 처리 상태(PENDING/DONE/FAILED) 및 완료 시 결과를 조회합니다."
    )
    @GetMapping("/contacts/jobs/{jobId}")
    suspend fun getContactsJob(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable jobId: String,
    ): ResponseEntity<ApiResult<ContactSyncJobStatusResponse>> {
        val data = contactSyncJobService.getJob(jobId, user.id)
        return ResponseEntity.ok(ApiResult(true, data))
    }
}
