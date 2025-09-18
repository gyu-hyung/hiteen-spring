package kr.jiasoft.hiteen.feature.interest.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "Interest-User", description = "사용자 관심사 관련 API")
@RestController
@RequestMapping("/api/interests/users")
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class InterestUserController(
    private val service: InterestUserService
) {


    @Operation(summary = "사용자 관심사 조회", description = "특정 사용자의 관심사 목록을 조회합니다.")
    @GetMapping
    suspend fun list(
        @Parameter(description = "조회할 사용자 UID") @RequestParam userUid: String
    ) = ResponseEntity.ok(ApiResult.success(service.getUserInterests(userUid)))


    @Operation(summary = "사용자 관심사 등록", description = "로그인한 사용자에게 관심사를 등록합니다.")
    @PostMapping
    suspend fun addInterest(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "등록할 관심사 ID") interestId: Long
    ) = ResponseEntity.ok(service.addInterestToUser(user, interestId))


    @Operation(summary = "사용자 관심사 삭제", description = "로그인한 사용자의 특정 관심사를 삭제합니다.")
    @DeleteMapping("/{interestId}")
    suspend fun delete(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "삭제할 관심사 ID") @RequestParam interestId: Long
    ) = ResponseEntity.ok(ApiResult.success(service.removeInterestFromUser(user.id, interestId)))


    @Operation(summary = "사용자 관심사 전체 삭제", description = "로그인한 사용자의 모든 관심사를 초기화합니다.")
    @DeleteMapping("/reset")
    suspend fun clear(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = ResponseEntity.ok(ApiResult.success(service.clearUserInterests(user.id)))


    @Operation(summary = "친구 추천", description = "관심사 기반으로 친구를 추천받습니다.")
    @GetMapping("/recommend")
    suspend fun recommend(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult.success(service.recommendFriend(user)))


    @Operation(summary = "추천 친구 패스", description = "추천된 친구를 패스합니다.")
    @PostMapping("/pass")
    suspend fun pass(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "패스할 대상 사용자 ID") @RequestParam targetUserId: Long
    ) = ResponseEntity.ok(ApiResult.success(service.passFriend(user, targetUserId)))
}
