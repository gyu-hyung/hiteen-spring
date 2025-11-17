package kr.jiasoft.hiteen.feature.chat.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.chat.dto.SendMessageRequest
import kr.jiasoft.hiteen.feature.user.app.UserReader
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
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
 *  websocat --ping-interval=20 "ws://49.247.169.182:30080/ws/chat?room=8921f90a-609c-4dd5-a7b0-fe25321b1e7c&token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTA5NTM5MzYzNyIsImlhdCI6MTc2MzM0NDg3NSwiZXhwIjoxNzY0MjA4ODc1fQ.Iwj9PnUVkQ8w83dCXh1vBhTjn7334OhuW_QO5RbPArutqxQ0GXL7QdGYbbiSefYvt-FtnqCc8MndTpMpjS6tnQ"
 *  websocat --ping-interval=20 "ws://49.247.169.182:30080/ws/chat?room=8921f90a-609c-4dd5-a7b0-fe25321b1e7c&token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTAyMjIyMjIyMiIsImlhdCI6MTc2MzM0NDg4OSwiZXhwIjoxNzY0MjA4ODg5fQ.Fr3fXHjBI6injuYE751TfyFI_YsETxZDZ2rBegfjVtUBy373hcGjJW_aq9VfY0vhYtrD7cKA2yAZU6b17p7RIA"
 *  {"type":"send","content":"CLI에서 보냄!","clientMsgId":"cli-test-1","assetUids":[]}
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

            val user = userReader.findByUsername(username) ?: throw IllegalStateException("user not found: $username")

            // 방 멤버 여부 검증 (없으면 403 유사 종료)
            chatService.assertMember(roomUid, user.id)

            AuthContext(roomUid, user, username)
        }.onErrorResume {
            session.send(Mono.just(session.textMessage(errorJson("auth_failed", it.message))))
                .then(session.close(CloseStatus.POLICY_VIOLATION))
                .then(Mono.error(it))

        }

        return authMono.flatMap { ctx ->
            val greetings: Mono<String> =
                chatHub.memberCountMono(ctx.roomUid)
                    .map { mc ->
                        mapper.writeValueAsString(
                            mapOf(
                                "type" to "hello",
                                "roomUid" to ctx.roomUid.toString(),
                                "userUid" to ctx.user.uid.toString(),
                                "members" to mc
                            )
                        )
                    }

            val broadcast: Flux<String> = chatHub.subscribe(ctx.roomUid)

            val outgoing: Flux<WebSocketMessage> =
                Flux.concat(greetings, broadcast).map { session.textMessage(it) }
                .doOnSubscribe { chatHub.join(ctx.roomUid, ctx.user.id, ctx.user.uid) }
                .doFinally { chatHub.leave(ctx.roomUid, ctx.user.id, ctx.user.uid) }

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
                val msgUid = chatService.sendMessage(ctx.roomUid, ctx.user, SendMessageRequest(content), emptyList())

//                val assetUids: List<UUID> =
//                    node.get("assetUids")?.let { arr: JsonNode ->
//                        arr.elements().asSequence()
//                            .mapNotNull { it.asText(null) }
//                            .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
//                            .toList()
//                    } ?: emptyList()

                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "message",
                        "roomUid" to ctx.roomUid.toString(),
                        "messageUid" to msgUid.toString(),
                        "userId" to ctx.user.id,
                        "senderUid" to ctx.user.uid.toString(),
                        "content" to content,
//                        "assetUids" to assetUids.map { it.toString() },
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
                    ctx.user,
                    SendMessageRequest(content = null, code),
                    emptyList()
                )

                // 즉시 WS 브로드캐스트(서버에서 이미 이모지 전용 payload를 publish했으면 생략 가능)
                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "emoji",
                        "roomUid" to ctx.roomUid.toString(),
                        "messageUid" to msgUid.toString(),
                        "senderUid" to ctx.user.uid.toString(),
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
                        "userId" to ctx.user.id,
                        "userUid" to ctx.user.uid.toString(),
                        "isTyping" to isTyping
                    )
                )
                chatHub.publish(ctx.roomUid, payload)
            }

            //{"type":"read","lastMessageUid":"8d0c9302-5382-449f-9cdb-ec40d0bb9251"}
            "read" -> {
                val lastMessageUid = node.get("lastMessageUid")?.asText() ?: return@mono
                chatService.markRead(ctx.roomUid, ctx.user, UUID.fromString(lastMessageUid))
                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "read",
                        "roomUid" to ctx.roomUid.toString(),
                        "userId" to ctx.user.id,
                        "userUid" to ctx.user.uid.toString(),
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

    private data class AuthContext(val roomUid: UUID, val user: UserEntity, val username: String)

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