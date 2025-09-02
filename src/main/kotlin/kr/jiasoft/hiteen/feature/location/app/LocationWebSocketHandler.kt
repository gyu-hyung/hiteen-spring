package kr.jiasoft.hiteen.feature.location.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.chat.domain.UserReader
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.time.OffsetDateTime

@Component
class LocationWebSocketHandler(
    private val jwt: JwtProvider,
    private val userReader: UserReader,
    private val authz: LocationAuthorizationServiceImpl,
    private val hub: LocationHub,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
) : WebSocketHandler {

    override fun handle(session: WebSocketSession): Mono<Void> {
        val params = session.handshakeInfo.uri.query?.let(::parseQuery) ?: emptyMap()
        val usersParam = params["users"]?.firstOrNull().orEmpty() // 콤마로 구분된 userUid 목록
        val tokenParam = params["token"]?.firstOrNull().orEmpty()

        if (usersParam.isBlank() || tokenParam.isBlank()) {
            return closeWithError(session, "bad_request", "users and token are required")
        }

        val token = BearerToken(tokenParam.removePrefix("Bearer ").trim())

        // 인증 + 인가
        val ctxMono = mono {
            val jws = jwt.parseAndValidateOrThrow(token)
            val requesterUsername = jws.payload.subject ?: error("no-subject")
            val requesterId = userReader.findIdByUsername(requesterUsername)
                ?: error("user not found: $requesterUsername")

            val userUids = usersParam.split(",")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .take(150)

            if (userUids.isEmpty()) error("no users")

            authz.assertCanSubscribe(requesterId, userUids) // 필요 정책대로 구현

            LocationCtx(requesterId, requesterUsername, userUids)
        }.onErrorResume { e ->
            closeWithError(session, "auth_failed", e.message).then(Mono.error(e))
        }

        return ctxMono.flatMap { ctx ->
            // hello 1회
            val hello = mapOf(
                "type" to "hello",
                "users" to ctx.userUids,
                "serverTime" to OffsetDateTime.now().toString(),
                "version" to "loc-ws/1.0"
            )
            val helloMono = Mono.just(session.textMessage(mapper.writeValueAsString(hello)))

            // 실시간 구독 (여러 유저 병합)
            val live: Flux<String> =
                hub.subscribeUsers(ctx.userUids)
                    .map { payload ->
                        // Redis에서 넘어온 payload가 이미 JSON이라고 가정.
                        // userUid를 함께 보내고 싶으면, 발행 측에서 포함시키거나 여기서 래핑해도 됨.
                        payload
                    }

            val outgoing: Flux<WebSocketMessage> =
                Flux.concat(helloMono, live.map(session::textMessage))
                    .onBackpressureBuffer(1024, {}, BufferOverflowStrategy.DROP_OLDEST)

            val incoming = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap { raw ->
                    when (mapper.readTree(raw).get("type")?.asText()) {
                        "ping" -> session.send(Mono.just(session.textMessage("""{"type":"pong"}""")))
                        else -> Mono.empty()
                    }
                }
                .onErrorResume { e ->
                    session.send(Mono.just(session.textMessage(errorJson("recv_error", e.message)))).then()
                }
                .then()

            session.send(outgoing).and(incoming)
        }
    }

    private data class LocationCtx(val requesterId: Long, val username: String, val userUids: List<String>)

    private fun parseQuery(q: String): Map<String, List<String>> =
        q.split("&").mapNotNull {
            val i = it.indexOf('=')
            if (i < 0) null else URLDecoder.decode(it.substring(0, i), Charsets.UTF_8) to
                    URLDecoder.decode(it.substring(i + 1), Charsets.UTF_8)
        }.groupBy({ it.first }, { it.second })

    private fun closeWithError(session: WebSocketSession, code: String, msg: String?): Mono<Void> =
        session.send(Mono.just(session.textMessage(errorJson(code, msg))))
            .then(session.close(CloseStatus.POLICY_VIOLATION))

    private fun errorJson(code: String, message: String?): String =
        """{"type":"error","code":"$code","message":${mapper.writeValueAsString(message ?: "")}}"""
}
