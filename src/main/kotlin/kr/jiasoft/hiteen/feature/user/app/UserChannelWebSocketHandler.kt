package kr.jiasoft.hiteen.feature.user.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.chat.app.ChatHub
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.*
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Component
class UserChannelWebSocketHandler(
    private val jwt: JwtProvider,
    private val chatHub: ChatHub,
    private val userReader: UserReader,
    private val mapper: ObjectMapper = jacksonObjectMapper()
) : WebSocketHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val params = session.handshakeInfo.uri.query?.let { parseQuery(it) } ?: emptyMap()
        val tokenParam = params["token"]?.firstOrNull()

        if (tokenParam.isNullOrBlank()) {
            return session.close(CloseStatus.BAD_DATA)
        }

        val tokenStr = tokenParam.removePrefix("Bearer ").trim()
        val token = BearerToken(tokenStr)

        // 인증
        val authMono = mono {
            val jws = jwt.parseAndValidateOrThrow(token)
            val username = jws.payload.subject ?: throw IllegalStateException("no-subject")

            val user = userReader.findByUsername(username)
                ?: throw IllegalStateException("user not found: $username")

            UserCtx(user, username)
        }

        return authMono.flatMap { ctx ->
            val sink = Sinks.many().multicast().onBackpressureBuffer<String>()
            val userSubscription = AtomicReference<Disposable?>()

            val outgoing: Flux<WebSocketMessage> =
                sink.asFlux().map { session.textMessage(it) }
                    .doOnSubscribe {
                        // Redis 사용자 notify 채널 구독
                        if (userSubscription.get() == null) {
                            val d = chatHub.subscribeUserNotify(ctx.user.uid)
                                .subscribe { msg -> sink.tryEmitNext(msg) }
                            userSubscription.set(d)
                            log.debug("user personal ws connected userUid={}", ctx.user.uid)
                        }
                    }.doFinally {
                        userSubscription.getAndSet(null)?.dispose()
                        log.debug("user personal ws disconnected userUid={}", ctx.user.uid)
                    }

            val incoming = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap { handleClientMessage(session, ctx, it) }
                .onErrorResume { e ->
                    session.send(Mono.just(session.textMessage(errorJson("recv_error", e.message))))
                }
                .then()

            session.send(outgoing).and(incoming)
        }
    }

    private fun handleClientMessage(
        session: WebSocketSession,
        ctx: UserCtx,
        raw: String,
    ): Mono<Void> = mono {
        val node = mapper.readTree(raw)
        val type = node.get("type")?.asText() ?: return@mono

        when (type) {
            "ping" -> {
                val pong = mapper.writeValueAsString(mapOf("type" to "pong"))
                session.send(Mono.just(session.textMessage(pong))).subscribe()
            }

            /** DM / push 이벤트 처리 예시 */
            "chat" -> {
                val targetUid = UUID.fromString(node.get("data")?.get("targetUid")?.asText())
                val message = node.get("data")?.get("message")?.asText() ?: return@mono

                val payload = mapper.writeValueAsString(
                    mapOf("type" to "dm", "from" to ctx.user.uid.toString(), "message" to message)
                )
                chatHub.publishUserNotify(targetUid, payload)
            }

            else -> {
                val err = errorJson("bad_type", "Unsupported type: $type")
                session.send(Mono.just(session.textMessage(err))).subscribe()
            }
        }
    }.then()

    private data class UserCtx(val user: UserEntity, val username: String)

    private fun parseQuery(q: String): Map<String, List<String>> =
        q.split("&").mapNotNull {
            val idx = it.indexOf("=")
            if (idx < 0) null else {
                val k = URLDecoder.decode(it.substring(0, idx), StandardCharsets.UTF_8)
                val v = URLDecoder.decode(it.substring(idx + 1), StandardCharsets.UTF_8)
                k to v
            }
        }.groupBy({ it.first }, { it.second })

    private fun errorJson(code: String, message: String?): String =
        """{"type":"error","code":"$code","message":${mapper.writeValueAsString(message ?: "")}}"""
}
