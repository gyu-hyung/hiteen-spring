package kr.jiasoft.hiteen.feature.chat.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.chat.domain.UserReader
import kr.jiasoft.hiteen.feature.chat.dto.SendMessageRequest
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * ws://{host}/ws/chat?room={roomUid}&token=Bearer%20{JWT}
 */
@Component
class ChatWebSocketHandler(
    private val jwt: JwtProvider,
    private val chatHub: ChatHub,
    private val chatService: ChatService,
    private val userReader: UserReader,
    private val mapper: ObjectMapper = jacksonObjectMapper()
) : WebSocketHandler {


    //socket 연결 시
    override fun handle(session: WebSocketSession): Mono<Void> {
        val params = session.handshakeInfo.uri.query?.let { parseQuery(it) } ?: emptyMap()
        val roomParam = params["room"]?.firstOrNull()
        val tokenParam = params["token"]?.firstOrNull()

        if (roomParam.isNullOrBlank() || tokenParam.isNullOrBlank()) {
            return session.close(CloseStatus.BAD_DATA)
        }

        val roomUid = try { UUID.fromString(roomParam) } catch (_: Exception) { return session.close(CloseStatus.BAD_DATA) }
        val tokenStr = tokenParam.removePrefix("Bearer ").trim()
        val token = BearerToken(tokenStr)

        // 인증 (JWT 유효성 + 사용자 조회)
        val authMono = mono {
            val jws = jwt.parseAndValidateOrThrow(token) // 실패 시 JwtException
            val username = jws.payload.subject ?: throw IllegalStateException("no-subject")
            val userId = userReader.findIdByUsername(username) ?: throw IllegalStateException("user not found: $username")
            val userUid = userReader.findUidById(username) ?: throw IllegalStateException("user uid not found: $username")

            // 방 멤버 여부 검증 (없으면 403 유사 종료)
            chatService.assertMember(roomUid, userId)

            AuthContext(roomUid, userId, userUid, username)
        }.onErrorResume {
            session.send(Mono.just(session.textMessage(errorJson("auth_failed", it.message))))
                .then(session.close(CloseStatus.POLICY_VIOLATION))
                .then(Mono.error<AuthContext>(it))

        }

        return authMono.flatMap { ctx ->
            val greetings: Mono<String> =
                chatHub.memberCountMono(ctx.roomUid)
                    .map { mc ->
                        mapper.writeValueAsString(
                            mapOf(
                                "type" to "hello",
                                "roomUid" to ctx.roomUid.toString(),
                                "userUid" to ctx.userUid.toString(),
                                "members" to mc
                            )
                        )
                    }

            val broadcast: Flux<String> = chatHub.subscribe(ctx.roomUid)

            val outgoing: Flux<WebSocketMessage> =
                Flux.concat(greetings, broadcast)
                    .map { session.textMessage(it) }
                    .doOnSubscribe { chatHub.join(ctx.roomUid, ctx.userId, ctx.userUid) }
                    .doFinally { chatHub.leave(ctx.roomUid, ctx.userId, ctx.userUid) }

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

    // 메세지 수신
    private fun handleClientMessage(
        session: WebSocketSession,
        ctx: AuthContext,
        raw: String
    ): Mono<Void> = mono {
        val node = mapper.readTree(raw)
        val type = node.get("type")?.asText() ?: return@mono

        when (type) {

            //{"type":"send","content":"채팅!!!","clientMsgId":"ibox-1","assetUids":[]}
            // TODO 안읽은 메세지가 있는 상태에서 메세지를 보냈을때 unread를 0으로 갱신할것인지?
            "send" -> {
                val content = node.get("content")?.asText()

                val clientMsgId = node.get("clientMsgId")?.asText()

                val assetUids: List<UUID> =
                    node.get("assetUids")?.let { arr: JsonNode ->
                        arr.elements().asSequence()
                            .mapNotNull { it.asText(null) }
                            .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
                            .toList()
                    } ?: emptyList()

                val msgUid = chatService.sendMessage(
                    ctx.roomUid,
                    ctx.userId,
                    SendMessageRequest(content = content, assetUids = assetUids)
                )

                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "message",
                        "roomUid" to ctx.roomUid.toString(),
                        "messageUid" to msgUid.toString(),
//                        "userId" to ctx.userId,
                        "senderUid" to ctx.userUid.toString(),
                        "content" to content,
                        "assetUids" to assetUids.map { it.toString() },
                        "clientMsgId" to clientMsgId
                    )
                )
                chatHub.publish(ctx.roomUid, payload)
            }

            //{"type":"emoji","emojiCode":"party_popper"}
            //{"type":"emoji","emojiCode":"laugh","clientMsgId":"123e4567-e89b-12d3-a456-426614174000"}
            //{"type":"emoji","emojiCode":"heart","meta":{"color":"red","size":"large"}}
            "emoji" -> {
                val code = node.get("emojiCode")?.asText() ?: return@mono

                val msgUid = chatService.sendMessage(
                    ctx.roomUid,
                    ctx.userId,
                    SendMessageRequest(kind = 2, emojiCode = code)
                )

                // 즉시 WS 브로드캐스트(서버에서 이미 이모지 전용 payload를 publish했으면 생략 가능)
                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "emoji",
                        "roomUid" to ctx.roomUid.toString(),
                        "messageUid" to msgUid.toString(),
                        "senderUid" to ctx.userUid.toString(),
                        "emojiCode" to code
                    )
                )
                chatHub.publish(ctx.roomUid, payload)
            }

            //{"type":"typing","isTyping":true}
            "typing" -> {
                val isTyping = node.get("isTyping")?.asBoolean() ?: true
                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "typing",
                        "roomUid" to ctx.roomUid.toString(),
//                        "userId" to ctx.userId,
                        "userUid" to ctx.userUid.toString(),
                        "isTyping" to isTyping
                    )
                )
                chatHub.publish(ctx.roomUid, payload)
            }

            //{"type":"read","lastMessageUid":"8d0c9302-5382-449f-9cdb-ec40d0bb9251"}
            "read" -> {
                val lastMessageUid = node.get("lastMessageUid")?.asText() ?: return@mono
                chatService.markRead(ctx.roomUid, ctx.userId, UUID.fromString(lastMessageUid))
                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "read",
                        "roomUid" to ctx.roomUid.toString(),
//                        "userId" to ctx.userId,
                        "userUid" to ctx.userUid.toString(),
                        "lastMessageUid" to lastMessageUid
                    )
                )
                chatHub.publish(ctx.roomUid, payload)
            }

            "ping" -> {
                // 단일 사용자에게만 회신
                val pong = mapper.writeValueAsString(mapOf("type" to "pong"))
                session.send(Mono.just(session.textMessage(pong))).subscribe()
            }

            else -> {
                // 알 수 없는 타입
                val err = errorJson("bad_type", "Unsupported type: $type")
                session.send(Mono.just(session.textMessage(err))).subscribe()
            }
        }
    }.then()

    private data class AuthContext(val roomUid: UUID, val userId: Long, val userUid: UUID, val username: String)

    private fun parseQuery(q: String): Map<String, List<String>> =
        q.split("&")
            .mapNotNull {
                val idx = it.indexOf('=')
                if (idx < 0) null
                else {
                    val k = URLDecoder.decode(it.substring(0, idx), StandardCharsets.UTF_8)
                    val v = URLDecoder.decode(it.substring(idx + 1), StandardCharsets.UTF_8)
                    k to v
                }
            }
            .groupBy({ it.first }, { it.second })

    private fun errorJson(code: String, message: String?): String =
        """{"type":"error","code":"$code","message":${mapper.writeValueAsString(message ?: "")}}"""
}
