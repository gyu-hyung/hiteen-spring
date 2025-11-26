package kr.jiasoft.hiteen.feature.chat.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.config.websocket.RedisChannelPattern
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.chat.dto.SendMessageRequest
import kr.jiasoft.hiteen.feature.chat.infra.ChatUserRepository
import kr.jiasoft.hiteen.feature.soketi.app.ChannelAuthorizationService
import kr.jiasoft.hiteen.feature.user.app.UserReader
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.Disposable
import reactor.core.publisher.BufferOverflowStrategy
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ✅ 새로운 프로토콜

 WebSocket 연결:
   ws://{host}/ws/chat?room={roomUid}&token=Bearer%20{JWT}
websocat --ping-interval=20 "ws://localhost:8080/ws/chat?room=fefe5b56-6dfc-455d-ab1e-935a9bb63c03&token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTA5NTM5MzYzNyIsImlhdCI6MTc2MzMzOTY4NSwiZXhwIjoxNzY0MjAzNjg1fQ.KB6e_w3L5k22L9EqkYjGIBOQshxwccRrOVVPYhtkiIYO8pJ9vfsQ1bmMzpumelNbFPlDAG8_jsYqwLeIoK0jUg"
websocat --ping-interval=20 "ws://localhost:8080/ws/chat?room=fefe5b56-6dfc-455d-ab1e-935a9bb63c03&token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTAyMjIyMjIyMiIsImlhdCI6MTc2MzMzOTczOSwiZXhwIjoxNzY0MjAzNzM5fQ.A6_vqyr5XmLsUJ65wteGEz488CxpX86x46fKB-g_872AYeg-RLiNxqInuM4KBKnHnVU_tUcf5jteWmOABhCKRA"
websocat --ping-interval=20 "ws://localhost:8080/ws/chat?token=Bearer%20eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTAzMzMzMzMzMyIsImlhdCI6MTc2MzQ0MTk2MiwiZXhwIjoxNzY0MzA1OTYyfQ.wQilm0xLD2OgVUGCNzmTSZtMgrhhXWGop2b-3Kf6DPIvFr15m7VDER_JbvnQlQ5V1I0jp46BL7-p6Oj4CUMXtw"
 * 클라이언트 → 서버 메시지 예시:

1) 채팅방 목록 진입 / 이탈
    { "type": "list_subscribe" }
    { "type": "list_unsubscribe" }

2) 방 입장 / 퇴장
    { "type": "join", "data": { "roomUid": "c84fa195-4491-4bea-af11-073f292d5472" } }
    { "type": "leave", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03" } }

3) 메시지 전송
    { "type": "send", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03", "content": "...", "emojiCode": "E_001"} }
    { "type": "send", "data": { "roomUid": "c84fa195-4491-4bea-af11-073f292d5472", "content": "...", "emojiCode": "E_001"} }

4) 타이핑
    { "type": "typing", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03", "isTyping": true } }

5) 읽음 처리
    { "type": "read", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03", "lastMessageUid": "UUID" } }
 */
@Component
class ChatWebSocketHandler(
    private val jwt: JwtProvider,
    private val chatHub: ChatHub,
    private val chatService: ChatService,
    private val chatUserRepository: ChatUserRepository,
    private val userReader: UserReader,
    private val channelAuth: ChannelAuthorizationService,
    private val mapper: ObjectMapper = jacksonObjectMapper()
) : WebSocketHandler {


    override fun handle(session: WebSocketSession): Mono<Void> {
        val params = session.handshakeInfo.uri.query?.let { parseQuery(it) } ?: emptyMap()
        val roomParam = params["room"]?.firstOrNull()
        val tokenParam = params["token"]?.firstOrNull()

        if (roomParam.isNullOrBlank() || tokenParam.isNullOrBlank()) {
            return session.close(CloseStatus.BAD_DATA)
        }

        val roomUid = runCatching { UUID.fromString(roomParam) }.getOrNull()
            ?: return session.close(CloseStatus.BAD_DATA)
        val tokenStr = tokenParam.removePrefix("Bearer ").trim()
        val token = BearerToken(tokenStr)

        // 인증 (JWT 유효성 + 사용자 조회)
        val authMono = mono {
            val jws = jwt.parseAndValidateOrThrow(token)
            val username = jws.payload.subject ?: throw IllegalStateException("no-subject")

            val user = userReader.findByUsername(username)
                ?: throw IllegalStateException("user not found: $username")

            // Redis 패턴 기반 채널 이름 생성
            val redisChannel = RedisChannelPattern.CHAT_ROOM.format(roomUid)

            // 🔥 권한 체크 replacing chatService.assertMember()
            val allowed = channelAuth.canSubscribe(user.id, redisChannel)

            if (!allowed) {
                throw IllegalAccessException("Access denied: $redisChannel")
            }

            ChatCtx(user, username, roomUid)
        }.onErrorResume {
            session.send(Mono.just(session.textMessage(errorJson("auth_failed", it.message))))
                .then(session.close(CloseStatus.POLICY_VIOLATION))
                .then(Mono.error(it))
        }

        return authMono.flatMap { ctx ->
            val sink = Sinks.many().multicast().onBackpressureBuffer<String>()
            val roomSubscriptions = ConcurrentHashMap<UUID, Disposable>()

            val outgoing = sink.asFlux()
                .map(session::textMessage)
                .onBackpressureBuffer(1024, {}, BufferOverflowStrategy.DROP_OLDEST)
                .doOnSubscribe {
                    chatHub.join(ctx.roomUid, ctx.user.id, ctx.user.uid)

                    val disposable = chatHub.subscribe(ctx.roomUid)
                        .subscribe { msg -> sink.tryEmitNext(msg) }

                    roomSubscriptions[ctx.roomUid] = disposable
                }

            val incoming = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap { handleClientMessage(session, ctx, it) }
                .onErrorResume { e ->
                    session.send(Mono.just(session.textMessage(errorJson("recv_error", e.message))))
                }
                .then()

            session.send(outgoing).and(incoming)
                // 🔥 WebSocket lifecycle 기준 cleanup
                .doFinally {
                    chatHub.leave(ctx.roomUid, ctx.user.id, ctx.user.uid)

                    roomSubscriptions.values.forEach { it.dispose() }
                    roomSubscriptions.clear()
                }
        }

    }

    // 클라이언트 → 서버 메시지 처리
    private fun handleClientMessage(
        session: WebSocketSession,
        ctx: ChatCtx,
        raw: String,
//        sink: Sinks.Many<String>,
//        roomSubscriptions: MutableMap<UUID, Disposable>,
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

            // { "type": "send", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03", "content": "...", "emojiCode": "E_001", "emojiCount": "1" } }
            "send" -> {
                if (dataNode == null) {
                    val err = errorJson("bad_format", "send requires 'data' field")
                    session.send(Mono.just(session.textMessage(err))).subscribe()
                    return@mono
                }

                val roomUidStr = dataNode.get("roomUid")?.asText() ?: return@mono
                val roomUid = runCatching { UUID.fromString(roomUidStr) }.getOrNull() ?: return@mono

                val request = mapper.treeToValue(dataNode, SendMessageRequest::class.java)
                val msgRes = runCatching { chatService.sendMessage(roomUid, ctx.user, request, emptyList()) }.getOrNull() ?: return@mono

                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "message",
                        "data" to msgRes
                    )
                )
                chatHub.publish(roomUid, payload)

                runCatching {
                    chatUserRepository.listActiveUserUidsByUid(roomUid).collect { userUid ->
                        chatHub.publishUserNotify(userUid, payload)
                    }
                }.getOrNull() ?: return@mono
            }

            // { "type": "typing", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03", "isTyping": true } }
            "typing" -> {
                val roomUidStr = dataNode?.get("roomUid")?.asText() ?: return@mono
                val roomUid = try { UUID.fromString(roomUidStr) } catch (_: Exception) { return@mono }
                val isTyping = dataNode.get("isTyping")?.asBoolean() ?: true

                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "typing",
                        "roomUid" to roomUid.toString(),
                        "userUid" to ctx.user.uid.toString(),
                        "isTyping" to isTyping
                    )
                )
                chatHub.publish(roomUid, payload)
            }

            // { "type": "read", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03", "lastMessageUid": "UUID" } }
            "read" -> {
                val roomUidStr = dataNode?.get("roomUid")?.asText() ?: return@mono
                val roomUid = try { UUID.fromString(roomUidStr) } catch (_: Exception) { return@mono }
                val lastMessageUidStr = dataNode.get("lastMessageUid")?.asText() ?: return@mono
                val lastMessageUid = try { UUID.fromString(lastMessageUidStr) } catch (_: Exception) { return@mono }

                chatService.markRead(roomUid, ctx.user, lastMessageUid)

                val payload = mapper.writeValueAsString(
                    mapOf(
                        "type" to "read",
                        "roomUid" to roomUid.toString(),
                        "userUid" to ctx.user.uid.toString(),
                        "lastMessageUid" to lastMessageUid.toString()
                    )
                )
                chatHub.publish(roomUid, payload)
            }

            // 채팅방 목록 화면 진입
            // { "type": "list_subscribe" }
//            "list_subscribe" -> {
//                if (listSubscriptionRef.get() == null) {
//                    val d = chatHub.subscribeUserNotify(ctx.user.uid)
//                        .subscribe { msg -> sink.tryEmitNext(msg) }
//                    listSubscriptionRef.set(d)
//                    log.debug("userId={} list_subscribe", ctx.user.nickname)
//                }
//            }

            // 채팅방 목록 화면 이탈
            // { "type": "list_unsubscribe" }
//            "list_unsubscribe" -> {
//                listSubscriptionRef.getAndSet(null)?.dispose()
//                log.debug("userId={} list_unsubscribe", ctx.user.nickname)
//            }


            // ===================== multi room =========================

            // 채팅방 입장
//             { "type": "join", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03" } }
//            "join" -> {
//                val roomUidStr = dataNode?.get("roomUid")?.asText() ?: return@mono
//                val roomUid = try { UUID.fromString(roomUidStr) } catch (_: Exception) { return@mono }
//
//                // 방 멤버 여부 검증
//                chatService.assertMember(roomUid, ctx.user.id)
//
//                // 분산 presence + system join 메시지
//                chatHub.join(roomUid, ctx.user.id, ctx.user.uid)
//
//                // 이 세션이 해당 방의 메시지를 수신하도록 Redis 구독 추가
//                if (!roomSubscriptions.containsKey(roomUid)) {
//                    val d = chatHub.subscribe(roomUid)
//                        .subscribe { msg -> sink.tryEmitNext(msg) }
//                    roomSubscriptions[roomUid] = d
//                }
//
//                // hello 메시지 (선택)
//                chatHub.memberCountMono(roomUid)
//                    .map { mc ->
//                        mapper.writeValueAsString(
//                            mapOf(
//                                "type" to "hello",
//                                "roomUid" to roomUid.toString(),
//                                "userUid" to ctx.user.uid.toString(),
//                                "members" to mc
//                            )
//                        )
//                    }
//                    .subscribe { json -> sink.tryEmitNext(json) }
//            }
//
//            // 채팅방 퇴장
////             { "type": "leave", "data": { "roomUid": "fefe5b56-6dfc-455d-ab1e-935a9bb63c03" } }
//            "leave" -> {
//                val roomUidStr = dataNode?.get("roomUid")?.asText() ?: return@mono
//                val roomUid = try { UUID.fromString(roomUidStr) } catch (_: Exception) { return@mono }
//
//                chatHub.leave(roomUid, ctx.user.id, ctx.user.uid)
//                roomSubscriptions.remove(roomUid)?.dispose()
//            }
            // ===================== multi room =========================

            else -> {
                val err = errorJson("bad_type", "Unsupported type: $type")
                session.send(Mono.just(session.textMessage(err))).subscribe()
            }
        }
    }.then()

    private data class ChatCtx(val user: UserEntity, val username: String, val roomUid: UUID)

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

