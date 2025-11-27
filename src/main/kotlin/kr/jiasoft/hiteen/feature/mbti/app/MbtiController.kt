package kr.jiasoft.hiteen.feature.mbti.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiAnswerRequest
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiQuestion
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/mbti")
@Tag(name = "MBTI", description = "MBTI 검사 API")
class MbtiController(
    private val mbtiService: MbtiService,
    private val userRepository: UserRepository,
    private val pointService: PointService,
) {

    @Operation(summary = "질문 목록 조회")
    @GetMapping("/questions")
    suspend fun questions(): ResponseEntity<ApiResult<List<MbtiQuestion>>> {
        val questions = mbtiService.getQuestions()
        return ResponseEntity.ok(ApiResult.success(questions))
    }

    @Operation(summary = "MBTI 결과 계산")
    @PostMapping("/results")
    suspend fun results(
        @RequestBody req: MbtiAnswerRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val result = mbtiService.calculateAndSave(user.id, req.answers)
        userRepository.updateMbti(user.id, result["result"] as String)

        if(!user.mbti.isNullOrBlank()) {
            pointService.applyPolicy(user.id, PointPolicy.MBTI_TEST)
        }

        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(summary = "MBTI 상세 결과 조회")
    @GetMapping("/view/{userUid}")
    suspend fun view(
        @PathVariable userUid: String
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val detail = mbtiService.viewResult(userUid)
        return ResponseEntity.ok(ApiResult.success(detail))
    }

}
