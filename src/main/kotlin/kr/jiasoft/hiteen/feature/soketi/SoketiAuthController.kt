package kr.jiasoft.hiteen.feature.soketi

import com.fasterxml.jackson.annotation.JsonProperty
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/broadcasting")
class SoketiAuthController(
    private val jwtProvider: JwtProvider,
    @field:Value("\${soketi.app-key}") private val appKey: String,
    @field:Value("\${soketi.app-secret}") private val appSecret: String,
) {

    data class AuthRequest(
        @param:JsonProperty("channel_name")
        val channelName: String?,
        @param:JsonProperty("socket_id")
        val socketId: String?,
        val channelData: String? = null
    )

    data class AuthRequest2(
        val channel_name: String? = null,
        val socket_id: String? = null
    )

    @PostMapping("/auth2")
    fun authorize2(
        @ModelAttribute formReq: AuthRequest,
        @RequestBody(required = false) jsonReq: AuthRequest?
    ): Mono<ResponseEntity<Map<String, String>>> {
        val channelName = formReq.channelName ?: jsonReq?.channelName
        val socketId = formReq.socketId ?: jsonReq?.socketId
        var channelData = formReq.channelData ?: jsonReq?.channelData

        if (channelName.isNullOrBlank() || socketId.isNullOrBlank()) {
            return Mono.just(
                ResponseEntity.badRequest().body(mapOf("error" to "Missing channel_name or socket_id"))
            )
        }

        // Presence 채널일 경우 기본 channelData
        if (channelName.startsWith("presence-") && channelData.isNullOrBlank()) {
            channelData = """{"user_id":1,"user_info":{"name":"Test User","role":"tester"}}"""
        }

        val stringToSign =
            "$socketId:$channelName" + if (!channelData.isNullOrBlank()) ":$channelData" else ""

        // appSecret은 non-null → 바로 사용
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appSecret.toByteArray(), "HmacSHA256"))
        val signature = mac.doFinal(stringToSign.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val result = if (!channelData.isNullOrBlank()) {
            mapOf("auth" to "$appKey:$signature", "channel_data" to channelData)
        } else {
            mapOf("auth" to "$appKey:$signature")
        }

        return Mono.just(ResponseEntity.ok(result))
    }

    @PostMapping("/auth")
    fun authorize(
        @ModelAttribute formReq: AuthRequest2?,
        @RequestHeader("Authorization", required = false) authHeader: String?
    ): Mono<ResponseEntity<Map<String, String>>> {
        val channelName = formReq?.channel_name
        val socketId = formReq?.socket_id
        if (channelName.isNullOrBlank() || socketId.isNullOrBlank()) {
            return Mono.just(
                ResponseEntity.badRequest().body(mapOf("error" to "Missing channel_name or socket_id"))
            )
        }

        var username = "anonymous"
        var role = "guest"

        // JWT 인증
        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
            try {
                val bearer = BearerToken(authHeader.removePrefix("Bearer ").trim())
                val claims = jwtProvider.parseAndValidateOrThrow(bearer).payload
                username = claims.subject ?: "anonymous"
                role = claims["role"] as? String ?: "user"
            } catch (e: Exception) {
                return Mono.just(ResponseEntity.status(403).body(mapOf("error" to "Unauthorized")))
            }
        }

        var stringToSign = "$socketId:$channelName"
        var channelData: String? = null

        if (channelName.startsWith("presence-")) {
            channelData = """{"user_id":"$username","user_info":{"name":"$username","role":"$role"}}"""
            stringToSign = "$socketId:$channelName:$channelData"
        }

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appSecret.toByteArray(), "HmacSHA256"))
        val signature = mac.doFinal(stringToSign.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val result = if (channelData != null) {
            mapOf("auth" to "$appKey:$signature", "channel_data" to channelData)
        } else {
            mapOf("auth" to "$appKey:$signature")
        }

        return Mono.just(ResponseEntity.ok(result))
    }
}
