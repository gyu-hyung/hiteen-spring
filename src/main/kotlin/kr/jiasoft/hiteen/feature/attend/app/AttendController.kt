package kr.jiasoft.hiteen.feature.attend.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.attend.domain.AttendEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/attend")
class AttendController(
    private val attendService: AttendService
) {

    @Operation(summary = "출석 현황")
    @GetMapping
    suspend fun view(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<List<AttendEntity>>> {
        val data = attendService.view(user)
        return ResponseEntity.ok(ApiResult.success(data))
    }

    @Operation(summary = "출석하기")
    @PostMapping
    suspend fun save(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val result = attendService.attend(user)
        return result.fold(
            onSuccess = {
                val data = mapOf(
                    "day" to it.sumDay,         // 오늘 몇 일차 출석인지
                    "point" to it.point,        // 지급 포인트
                    "addPoint" to it.addPoint   // 추가 포인트 (보너스)
                )
                ResponseEntity.ok(ApiResult.success(data))
            },
            onFailure = {
                throw BusinessValidationException(
                    mapOf("attend.save" to (it.message ?: "출석 실패"))
                )
            }
        )
    }

}
