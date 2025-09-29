package kr.jiasoft.hiteen.feature.notification.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.notification.dto.NotificationResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kr.jiasoft.hiteen.feature.user.domain.UserEntity

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

//    @Operation(summary = "알림 조회", description = "푸시, 채팅, 공지, 이벤트, 받은 선물, 친구 요청 알림을 조회합니다.")
//    @GetMapping
//    suspend fun alerts(
//        @AuthenticationPrincipal(expression = "user") user: UserEntity
//    ): ResponseEntity<ApiResult<NotificationResponse>> {
//        val result = notificationService.getAlerts(user.id)
//        return ResponseEntity.ok(ApiResult.success(result))
//    }



}
