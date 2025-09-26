package kr.jiasoft.hiteen.feature.user.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.feature.user.dto.UserDetailRequest
import kr.jiasoft.hiteen.feature.user.dto.UserDetailResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "UserDetail", description = "회원 상세 API")
@RestController
@RequestMapping("/api/user-details")
class UserDetailController(
    private val service: UserDetailService
) {

    @Operation(summary = "내 상세정보 조회")
    @GetMapping("/me")
    suspend fun getMyDetail(@AuthenticationPrincipal(expression = "id") userId: Long): ResponseEntity<UserDetailResponse?> {
        return ResponseEntity.ok(service.getUserDetail(userId))
    }

    @Operation(summary = "내 상세정보 등록/수정")
    @PostMapping("/me")
    suspend fun upsertMyDetail(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @RequestBody req: UserDetailRequest
    ): ResponseEntity<UserDetailResponse> {
        return ResponseEntity.ok(service.upsertUserDetail(userId, req))
    }

    @Operation(summary = "내 상세정보 삭제")
    @DeleteMapping("/me")
    suspend fun deleteMyDetail(@AuthenticationPrincipal(expression = "id") userId: Long): ResponseEntity<Unit> {
        service.deleteUserDetail(userId)
        return ResponseEntity.ok(Unit)
    }
}
