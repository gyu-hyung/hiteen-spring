package kr.jiasoft.hiteen.feature.chat.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.user.app.UserReader
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Component
class InboxWebSocketHandler(
    private val jwt: JwtProvider,
    private val userReader: UserReader,
    private val inbox: InboxHub,
) : WebSocketHandler {

    private val mapper = jacksonObjectMapper()

    private data class Auth(val user: UserEntity)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val params = session.handshakeInfo.uri.query?.let { parseQuery(it) } ?: emptyMap()
        val tokenParam = params["token"]?.firstOrNull() ?: return session.close(CloseStatus.BAD_DATA)
        val token = BearerToken(tokenParam.removePrefix("Bearer ").trim())

        val authMono = mono {
            val jws = jwt.parseAndValidateOrThrow(token)
            val username = jws.payload.subject ?: error("no-subject")
            val user = userReader.findByUsername(username) ?: throw IllegalStateException("user not found: $username")
            Auth(user)
        }.onErrorResume {
            session.send(Mono.just(session.textMessage(error("auth_failed", it.message))))
                .then(session.close(CloseStatus.POLICY_VIOLATION))
                .then(Mono.error(it))
        }

        return authMono.flatMap { a ->
            val hello = mapper.writeValueAsString(mapOf(
                "type" to "hello",
//                "userId" to a.userId,
                "userUid" to a.user.uid,
            ))
            val greetings = Mono.just(hello)
            val stream: Flux<String> = inbox.subscribe(a.user.uid)

            val outgoing = Flux.concat(greetings, stream).map { session.textMessage(it) }
            val incoming = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap {
                    // 현재는 ping 정도만 필요
                    if (it.contains("\"type\":\"ping\"")) {
                        val pong = mapper.writeValueAsString(mapOf("type" to "pong"))
                        session.send(Mono.just(session.textMessage(pong)))
                    } else Mono.empty()
                }
                .onErrorResume { e ->
                    session.send(Mono.just(session.textMessage(error("recv_error", e.message))))
                }
                .then()

            session.send(outgoing).and(incoming)
        }
    }


    private fun parseQuery(q: String): Map<String, List<String>> =
        q.split("&").mapNotNull {
            val i = it.indexOf('=')
            if (i < 0) null else {
                URLDecoder.decode(it.substring(0, i), StandardCharsets.UTF_8) to
                        URLDecoder.decode(it.substring(i + 1), StandardCharsets.UTF_8)
            }
        }.groupBy({ it.first }, { it.second })

    private fun error(code: String, msg: String?) =
        """{"type":"error","code":"$code","message":${mapper.writeValueAsString(msg ?: "")}}"""
}