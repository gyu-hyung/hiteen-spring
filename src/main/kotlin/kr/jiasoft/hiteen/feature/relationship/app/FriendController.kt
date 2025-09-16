package kr.jiasoft.hiteen.feature.relationship.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.relationship.dto.ContactResponse
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipSearchItem
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.zip.GZIPInputStream

@RestController
@RequestMapping("/api/friends")
class FriendController(
    private val friendService: FriendService
) {

    /** 내 친구 목록 (수락됨)
     * TODO : 안개모드,
     */
    @GetMapping
    suspend fun list(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = ResponseEntity.ok(ApiResult(true, friendService.listFriends(user)))


    /** 검색 (유저 uid/username/nickname/email) */
    @GetMapping("/search")
    suspend fun search(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam q: String,
        @RequestParam(required = false, defaultValue = "30") limit: Int
    ): ResponseEntity<ApiResult<List<RelationshipSearchItem>>> {
        return ResponseEntity.ok(ApiResult(true, friendService.search(user, q, limit)))
    }


    /** 연락처로 친구 목록 조회 */
    @PostMapping("/contacts", consumes = [MediaType.TEXT_PLAIN_VALUE])
    suspend fun getContacts(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody rawContacts: String
    ) = ResponseEntity.ok(ApiResult(true,friendService.getContacts(user, rawContacts)))


    /** 연락처로 친구 목록 조회 upload */
    @PostMapping("/contacts/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun uploadContacts(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart("file") filePart: FilePart
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


        // 2. gzip 해제
        val rawContacts = GZIPInputStream(compressedBytes.inputStream())
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        // 3. 서비스 호출
        val response = friendService.getContacts(user, rawContacts)
        return ResponseEntity.ok(response)
    }


    /** 내가 보낸 대기중 요청 */
    @GetMapping("/requests/outgoing")
    suspend fun outgoing(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = ResponseEntity.ok(ApiResult(true,friendService.listOutgoing(user)))


    /** 내가 받은 대기중 요청 */
    @GetMapping("/requests/incoming")
    suspend fun incoming(@AuthenticationPrincipal(expression = "user") user: UserEntity)
            = ResponseEntity.ok(ApiResult(true,friendService.listIncoming(user)))


    /** 친구 요청 보내기: me -> {uid} */
    @PostMapping("/request/{userUid}")
    suspend fun request(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true,friendService.request(user, userUid)))


    /** 받은 요청 수락: {uid} -> me */
    @PostMapping("/accept/{userUid}")
    suspend fun accept(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true,friendService.accept(user, userUid)))


    /** 받은 요청 거절 */
    @PostMapping("/reject/{userUid}")
    suspend fun reject(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true,friendService.reject(user, userUid)))


    /** 내가 보낸 요청 취소 */
    @PostMapping("/cancel/{userUid}")
    suspend fun cancel(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true,friendService.cancel(user, userUid)))


    /** 친구 끊기 */
    @PostMapping("/unfriend/{userUid}")
    suspend fun unfriend(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable userUid: String
    ) = ResponseEntity.ok(ApiResult(true, friendService.unfriend(user, userUid)))


}
