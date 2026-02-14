package kr.jiasoft.hiteen.feature.block.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.block.dto.BlockUserRequest
import kr.jiasoft.hiteen.feature.block.dto.BlockedUserResponse
import kr.jiasoft.hiteen.feature.block.dto.UnblockUserRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "사용자 차단", description = "사용자 차단 관련 API")
@RestController
@RequestMapping("/api/app/blocks")
class UserBlockController(
    private val userBlockService: UserBlockService,
) {

    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단합니다. 차단하면 해당 사용자의 게시글, 투표, 댓글이 보이지 않습니다.")
    @PostMapping
    suspend fun blockUser(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody request: BlockUserRequest,
    ): ResponseEntity<ApiResult<Any>> {
        userBlockService.blockUser(user.id, request.targetUid, request.reason)
        return ResponseEntity.ok(ApiResult.success("차단되었습니다."))
    }

    @Operation(summary = "사용자 차단 해제", description = "차단한 사용자의 차단을 해제합니다.")
    @DeleteMapping
    suspend fun unblockUser(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody request: UnblockUserRequest,
    ): ResponseEntity<ApiResult<Any>> {
        userBlockService.unblockUser(user.id, request.targetUid)
        return ResponseEntity.ok(ApiResult.success("차단이 해제되었습니다."))
    }

    @Operation(summary = "차단한 사용자 목록 조회", description = "내가 차단한 사용자 목록을 조회합니다.")
    @GetMapping
    suspend fun getBlockedUsers(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<List<BlockedUserResponse>>> {
        val blockedUsers = userBlockService.getBlockedUsers(user.id)
        return ResponseEntity.ok(ApiResult.success(blockedUsers))
    }

    @Operation(summary = "차단 여부 확인", description = "특정 사용자를 차단했는지 확인합니다.")
    @GetMapping("/check/{targetUid}")
    suspend fun checkBlocked(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable targetUid: String,
    ): ResponseEntity<ApiResult<Map<String, Boolean>>> {
        val targetUser = userBlockService.getBlockedUsers(user.id)
            .find { it.blockedUserUid == targetUid }
        return ResponseEntity.ok(ApiResult.success(mapOf("blocked" to (targetUser != null))))
    }
}

