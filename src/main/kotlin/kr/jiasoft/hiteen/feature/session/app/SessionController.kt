package kr.jiasoft.hiteen.feature.session.app

import kr.jiasoft.hiteen.feature.session.domain.UserSession
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sessions")
class SessionController(
    private val sessionService: SessionService
) {

    @PostMapping("/start")
    suspend fun startSession(@AuthenticationPrincipal(expression = "user") user: UserEntity): ResponseEntity<UserSession> {
        val session = sessionService.startSession(user.id)
        return ResponseEntity.ok(session)
    }

    @PostMapping("/end")
    suspend fun endSession(@AuthenticationPrincipal(expression = "user") user: UserEntity): ResponseEntity<String> {
        sessionService.endSession(user.id)
        return ResponseEntity.ok("세션 종료 및 경험치 적립 완료")
    }
}
