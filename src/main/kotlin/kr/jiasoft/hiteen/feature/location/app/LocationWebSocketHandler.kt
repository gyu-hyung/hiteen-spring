package kr.jiasoft.hiteen.feature.location.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.location.dto.LocationRequest
import kr.jiasoft.hiteen.feature.user.app.UserReader
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference


/**
websocat --ping-interval=20 "ws://localhost:8080/ws/loc?users=6e330bdc-3062-4a14-80f2-a46e04278c5c,ade41de8-4276-4fb7-9473-cc69cf9e451f,ae67afaa-eb77-4480-a98b-08b48e4c197a&token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTA5NTM5MzYzNyIsImlhdCI6MTc2MzMzOTY4NSwiZXhwIjoxNzY0MjAzNjg1fQ.KB6e_w3L5k22L9EqkYjGIBOQshxwccRrOVVPYhtkiIYO8pJ9vfsQ1bmMzpumelNbFPlDAG8_jsYqwLeIoK0jUg"
websocat --ping-interval=20 "ws://localhost:8080/ws/loc?users=6e330bdc-3062-4a14-80f2-a46e04278c5c,ade41de8-4276-4fb7-9473-cc69cf9e451f&token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTA5NTM5MzYzNyIsImlhdCI6MTc2MzMzOTY4NSwiZXhwIjoxNzY0MjAzNjg1fQ.KB6e_w3L5k22L9EqkYjGIBOQshxwccRrOVVPYhtkiIYO8pJ9vfsQ1bmMzpumelNbFPlDAG8_jsYqwLeIoK0jUg"

websocat --ping-interval=20 "ws://localhost:8080/ws/loc?users=6e330bdc-3062-4a14-80f2-a46e04278c5c,ade41de8-4276-4fb7-9473-cc69cf9e451f,ae67afaa-eb77-4480-a98b-08b48e4c197a&token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTAzMzMzMzMzMyIsImlhdCI6MTc2MzMzOTc1OCwiZXhwIjoxNzY0MjAzNzU4fQ.FDSKojY9-_ujdau-dyQnWWAtNjJsRz0_UjOLXZAnpOsQ9unn0yiuXHJOB-UbEcweP6Rk0xb0_F6gl-TbqnWMWg"
 * */
@Component
class LocationWebSocketHandler(
    private val jwt: JwtProvider,
    private val userReader: UserReader,
    private val authz: LocationAuthorizationServiceImpl,
    private val hub: LocationHub,
    private val mapper: ObjectMapper = jacksonObjectMapper(),
    private val locationAppService: LocationAppService,
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
            val user = userReader.findByUsername(requesterUsername)
                ?: error("user not found: $requesterUsername")

            val userUids = usersParam.split(",")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .mapNotNull { runCatching { UUID.fromString(it.trim()) }.getOrNull() }
                .distinct()
                .take(150)

            if (userUids.isEmpty()) error("no users")

            authz.assertCanSubscribe(user.id, userUids) // 필요 정책대로 구현

            LocationCtx(user, userUids)
        }.onErrorResume { e ->
            closeWithError(session, "auth_failed", e.message).then(Mono.error(e))
        }

        return ctxMono.flatMap { ctx ->
            // hello 1회
            val hello = mapOf(
                "type" to "hello",
                "users" to ctx.userUids,
                "serverTime" to OffsetDateTime.now().toString(),
            )
            val helloMono = Mono.just(session.textMessage(mapper.writeValueAsString(hello)))

            val subscriptionRef = AtomicReference<Disposable?>()

            val outgoing: Flux<WebSocketMessage> =
                Flux.concat(
                    helloMono,
                    hub.subscribeUsers(ctx.userUids).map(session::textMessage)
                ).onBackpressureBuffer(1024, {}, BufferOverflowStrategy.DROP_OLDEST)

            val incoming = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap { raw -> handleClientMessage(session, ctx, raw) }
                .onErrorResume { e ->
                    session.send(Mono.just(session.textMessage(errorJson("recv_error", e.message)))).then()
                }
                .then()

            session.send(outgoing)
                .and(incoming)
                .doFinally {
                    subscriptionRef.getAndSet(null)?.dispose()
                }
        }
    }

    /** -------------------------
     *  메시지 수신 처리 (ChatWebSocketHandler 동일 구조)
     *  ------------------------- */
    private fun handleClientMessage(
        session: WebSocketSession,
        ctx: LocationCtx,
        raw: String
    ): Mono<Void> = mono {
        val node = mapper.readTree(raw)
        val type = node.get("type")?.asText() ?: return@mono
        val dataNode = node.get("data")

        when (type) {

            // { "type": "ping" }
            "ping" -> {
                val pong = mapper.writeValueAsString(mapOf("type" to "pong"))
                session.send(Mono.just(session.textMessage(pong))).subscribe()
            }

            /** 📍 위치 업데이트 요청 */
            // { "type":"send", "data": { "lat":37.56788,"lng":126.97806,"timestamp": 1922432318006, "accuracy":10 } }
            "send" -> {
                if (dataNode == null) {
                    session.send(Mono.just(session.textMessage(errorJson("bad_format", "data required")))).subscribe()
                    return@mono
                }

                // WS payload → 기존 HTTP DTO 매핑
                val request = mapper.treeToValue(dataNode, LocationRequest::class.java)

                // 기존 HTTP 서비스 로직 그대로 사용
                val history = locationAppService.saveLocation(ctx.user, request)

                // Broadcast payload
                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "location_updated",
                        "data" to history,
                    )
                )

                hub.publish(ctx.user.uid, payload)
            }

            else -> {
                val err = errorJson("bad_type", "Unsupported type: $type")
                session.send(Mono.just(session.textMessage(err))).subscribe()
            }
        }
    }.then()

    private data class LocationCtx(val user: UserEntity, val userUids: List<UUID>)

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
