package kr.jiasoft.hiteen.feature.interest.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestRegisterRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/interests")
class InterestController(
    private val interestService: InterestService
) {

    @PostMapping
    suspend fun create(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: InterestRegisterRequest
    ) : ResponseEntity<ApiResult<InterestEntity>> {
        val saved = interestService.createInterest(user, request)
        return ResponseEntity.ok(ApiResult.success(saved))
    }


    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: Long): ResponseEntity<ApiResult<InterestEntity>>
        = ResponseEntity.ok(ApiResult.success(interestService.getInterest(id)))


    @GetMapping
    suspend fun list() = ResponseEntity.ok(ApiResult.success(interestService.getAllInterests()))


    @PostMapping("/update")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: InterestRegisterRequest,
    ) = ResponseEntity.ok(ApiResult.success(interestService.updateInterest(user, request)))


    @PostMapping("/delete")
    suspend fun delete(@PathVariable id: Long, @RequestParam deletedId: Long)
        = ResponseEntity.ok(ApiResult.success(interestService.deleteInterest(id, deletedId)))
}
