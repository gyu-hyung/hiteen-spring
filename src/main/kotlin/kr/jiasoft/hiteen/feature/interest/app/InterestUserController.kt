package kr.jiasoft.hiteen.feature.interest.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/interests/users")
class InterestUserController(
    private val service: InterestUserService
) {

    /** 사용자 관심사 등록 */
    @PostMapping("/save")
    suspend fun addInterest(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam interestId: Long
    ) = ResponseEntity.ok(service.addInterestToUser(user, interestId))


    /** 사용자 관심사 조회 */
    @GetMapping
    suspend fun list(userUid: String)
        = ResponseEntity.ok(ApiResult.success(service.getUserInterests(userUid)))


    /** 사용자 관심사 삭제 */
    @PostMapping("/delete")
    suspend fun delete(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestParam interestId: Long
    ) = ResponseEntity.ok(ApiResult.success(service.removeInterestFromUser(user.id!!, interestId)))


    /** 사용자 관심사 전체 삭제 */
    @PostMapping("/reset")
    suspend fun clear(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) = ResponseEntity.ok(ApiResult.success(service.clearUserInterests(user.id!!)))
}
