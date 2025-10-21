package kr.jiasoft.hiteen.feature.soketi.app

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Tag(name = "Soketi Broadcasting", description = "Soketi/Pusher 인증 API")
@RestController
@RequestMapping("/broadcasting")
class SoketiAuthController(
    private val jwtProvider: JwtProvider,
    private val channelAuthService: ChannelAuthorizationService,
    @Value("\${soketi.app-key}") private val appKey: String,
    @Value("\${soketi.app-secret}") private val appSecret: String,
) {

    data class AuthRequest(
        @field:Schema(description = "채널 이름", example = "presence-chatroom-1")
        @param:JsonProperty("channel_name")
        val channelName: String?,
        @field:Schema(description = "소켓 ID", example = "1234.5678")
        @param:JsonProperty("socket_id")
        val socketId: String?,
        @field:Schema(description = "Presence 채널일 경우 사용자 데이터 JSON", example = """{"user_id":"42","user_info":{"name":"Alice","role":"admin"}}""")
        val channelData: String? = null
    )

    data class AuthRequest2(
        @Schema(description = "채널 이름", example = "private-updates")
        val channel_name: String? = null,
        @Schema(description = "소켓 ID", example = "9876.5432")
        val socket_id: String? = null
    )

    @Operation(
        summary = "소켓 인증 (JWT optional)",
        description = "Pusher/Soketi에서 private/presence 채널 접속시 호출되는 인증 엔드포인트입니다.\n" +
                "Authorization: Bearer <JWT> 헤더가 있으면 JWT claim 기반으로 사용자 정보를 포함시킵니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/auth")
    suspend fun authorize(
        @Parameter(hidden = true) @ModelAttribute formReq: AuthRequest2?,
        @RequestHeader("Authorization", required = false) authHeader: String?
    ): Mono<ResponseEntity<ApiResult<Map<String, String>>>> {
        val channelName = formReq?.channel_name
        val socketId = formReq?.socket_id
        if (channelName.isNullOrBlank() || socketId.isNullOrBlank()) {
            throw IllegalArgumentException("Missing channel_name or socket_id")
        }

        var username = "anonymous"
        var role = "guest"
        var userIdFromJwt: Long? = null

        // JWT 인증
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
            try {
                val bearer = BearerToken(authHeader.removePrefix("Bearer ").trim())
                val claims = jwtProvider.parseAndValidateOrThrow(bearer).payload
                username = claims.subject ?: "anonymous"
                role = claims["role"] as? String ?: "user"
                userIdFromJwt = claims["userId"] as? Long
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid JWT token")
            }
        }

        if (!channelAuthService.canSubscribe(userIdFromJwt, channelName)) {
            throw IllegalArgumentException("Forbidden channel access")
        }

        var stringToSign = "$socketId:$channelName"
        var channelData: String? = null

        if (channelName.startsWith("presence-")) {
            channelData = """{"user_id":"$username","user_info":{"name":"$username","role":"$role"}}"""
            stringToSign = "$socketId:$channelName:$channelData"
        }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appSecret.toByteArray(), "HmacSHA256"))
        val signature = mac.doFinal(stringToSign.toByteArray()).joinToString("") { "%02x".format(it) }

        val result = if (channelData != null) {
            mapOf("auth" to "$appKey:$signature", "channel_data" to channelData)
        } else {
            mapOf("auth" to "$appKey:$signature")
        }

        return Mono.just(ResponseEntity.ok(ApiResult.success(result)))
    }

//    @Operation(
//        summary = "소켓 인증 (단순 버전)",
//        description = "폼데이터/JSON 모두 허용하는 간단 인증 엔드포인트입니다."
//    )
//    @PostMapping("/auth2")
//    fun authorize2(
//        @Parameter(hidden = true) @ModelAttribute formReq: AuthRequest,
//        @Parameter(hidden = true) @RequestBody(required = false) jsonReq: AuthRequest?
//    ): Mono<ResponseEntity<Map<String, String>>> {
//        val channelName = formReq.channelName ?: jsonReq?.channelName
//        val socketId = formReq.socketId ?: jsonReq?.socketId
//        var channelData = formReq.channelData ?: jsonReq?.channelData
//
//        if (channelName.isNullOrBlank() || socketId.isNullOrBlank()) {
//            return Mono.just(ResponseEntity.badRequest().body(mapOf("error" to "Missing channel_name or socket_id")))
//        }
//
//        if (channelName.startsWith("presence-") && channelData.isNullOrBlank()) {
//            channelData = """{"user_id":1,"user_info":{"name":"Test User","role":"tester"}}"""
//        }
//
//        val stringToSign = "$socketId:$channelName" + if (!channelData.isNullOrBlank()) ":$channelData" else ""
//
//        val mac = Mac.getInstance("HmacSHA256")
//        mac.init(SecretKeySpec(appSecret.toByteArray(), "HmacSHA256"))
//        val signature = mac.doFinal(stringToSign.toByteArray()).joinToString("") { "%02x".format(it) }
//
//        val result = if (!channelData.isNullOrBlank()) {
//            mapOf("auth" to "$appKey:$signature", "channel_data" to channelData)
//        } else {
//            mapOf("auth" to "$appKey:$signature")
//        }
//
//        return Mono.just(ResponseEntity.ok(result))
//    }
}
