package kr.jiasoft.hiteen.feature.user.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserDetailRequest
import kr.jiasoft.hiteen.feature.user.dto.UserDetailResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@Tag(name = "UserDetail", description = "회원 상세 API")
@RestController
@RequestMapping("/api/user-details")
class UserDetailController(
    private val service: UserDetailService
) {

    @Operation(summary = "내 상세정보 조회")
    @GetMapping("/me")
    suspend fun getMyDetail(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
//        @Parameter(description = "조회할 사용자 UID", example = "6f9b90d6-96ca-49de-b9c2-b123e51ca7db")  @PathVariable userUid: UUID
    ): ResponseEntity<ApiResult<UserDetailResponse>> {
        return ResponseEntity.ok(ApiResult.success(service.getUserDetail(user.uid)))
    }

    @Operation(summary = "상세정보 조회 - ADMIN")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userUid}")
    suspend fun getDetail(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "조회할 사용자 UID", example = "6f9b90d6-96ca-49de-b9c2-b123e51ca7db")  @PathVariable userUid: UUID
    ): ResponseEntity<ApiResult<UserDetailResponse>> {
        return ResponseEntity.ok(ApiResult.success(service.getUserDetail(userUid)))
    }

    @Operation(summary = "내 상세정보 등록/수정")
    @PostMapping
    suspend fun upsertMyDetail(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "수정할 사용자 상세정보") req: UserDetailRequest
    ): ResponseEntity<ApiResult<UserDetailResponse>> {
        return ResponseEntity.ok(ApiResult.success(service.upsertUserDetail(user.uid, req)))
    }

    @Operation(summary = "내 상세정보 삭제")
    @DeleteMapping("/me")
    suspend fun deleteMyDetail(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "삭제할 사용자 UID", example = "6f9b90d6-96ca-49de-b9c2-b123e51ca7db")  @PathVariable userUid: UUID
    ): ResponseEntity<Unit> {
        service.deleteUserDetail(userUid)
        return ResponseEntity.ok(Unit)
    }
}
