package kr.jiasoft.hiteen.feature.push.app

import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/push")
class PushTestController(
    private val pushService: PushService
) {
    @PostMapping("/test")
    suspend fun sendPushTest(
        @RequestParam(required = false, defaultValue = "테스트 제목") title: String,
        @RequestParam(required = false, defaultValue = "테스트 메시지입니다!") message: String,
        @AuthenticationPrincipal(expression = "user") user : UserEntity
    ): String {
        val data = mapOf("title" to title, "message" to message)
        pushService.sendAndSavePush(
            userIds = listOf(user.id),
            data = data
        )
        return "푸시 전송 완료"
    }


    data class PayloadRequest(
        val to: String,
        val notification: NotificationPayload?,
        val data: Map<String, Any> = emptyMap()
    )

    data class NotificationPayload(
        val title: String,
        val body: String
    )

    @PostMapping("/send-payload")
    fun sendPayload(
        req: PayloadRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<Any> {
        // 이 예시는 실제 푸시 보내는 대신, 요청된 페이로드를 그대로 반환하는 하드코딩 + 로깅 버전임
        println("🔔 Received push payload request: $req for user ${user.id}")
        // 실제로 FCM 서버로 전달한다면, 여기에 send logic 삽입

        return ResponseEntity.ok(mapOf(
            "result" to "payload accepted",
            "to" to req.to,
            "notification" to req.notification,
            "data" to req.data
        ))
    }


}
