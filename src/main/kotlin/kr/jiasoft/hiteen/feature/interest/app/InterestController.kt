package kr.jiasoft.hiteen.feature.interest.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestRegisterRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Interest", description = "관심사 관련 API")
@RestController
@RequestMapping("/api/interests")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class InterestController(
    private val interestService: InterestService
) {

    @Operation(summary = "관심사 등록", description = "새로운 관심사를 등록합니다.")
    @PostMapping
    suspend fun create(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "관심사 등록 요청 DTO") interestRegisterRequest: InterestRegisterRequest
    ) : ResponseEntity<ApiResult<InterestEntity>> {
        val saved = interestService.createInterest(user, interestRegisterRequest)
        return ResponseEntity.ok(ApiResult.success(saved))
    }


    @Operation(summary = "관심사 상세 조회", description = "관심사 ID로 특정 관심사를 조회합니다.")
    @GetMapping("/{id}")
    suspend fun get(
        @Parameter(description = "관심사 ID") @PathVariable id: Long
    ): ResponseEntity<ApiResult<InterestEntity>> =
        ResponseEntity.ok(ApiResult.success(interestService.getInterest(id)))


    @Operation(summary = "관심사 전체 조회", description = "유저별 등록 여부 포함, 모든 관심사를 카테고리별로 조회합니다.")
    @GetMapping
    suspend fun list(
        @AuthenticationPrincipal(expression = "user") user: UserEntity) =
        ResponseEntity.ok(ApiResult.success(interestService.getAllInterestsByUser(user.id)))


    @Operation(summary = "관심사 수정", description = "특정 관심사를 수정합니다.")
    @PostMapping("/{id}")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "관심사 수정 요청 DTO") interestRegisterRequest: InterestRegisterRequest,
    ) = ResponseEntity.ok(ApiResult.success(interestService.updateInterest(user, interestRegisterRequest)))


    @Operation(summary = "관심사 삭제", description = "특정 관심사를 삭제합니다.")
    @DeleteMapping("/{id}")
    suspend fun delete(
        @Parameter(description = "삭제할 관심사 ID") @RequestParam id: Long,
        @Parameter(description = "삭제 수행자 ID") @RequestParam deletedId: Long
    ) = ResponseEntity.ok(ApiResult.success(interestService.deleteInterest(id, deletedId)))
}
