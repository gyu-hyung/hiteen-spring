package kr.jiasoft.hiteen.feature.mbti.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiAnswerRequest
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiQuestion
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
        val result = mbtiService.calculateResult(req.answers)
        userRepository.updateMbti(user.id, result["result"] as String)

        // TODO: 포인트 사용 처리 (예: MBTI 검사 결과를 볼 때 포인트 차감)

        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(summary = "MBTI 상세 결과 조회")
    @GetMapping("/view")
    suspend fun view(
        @RequestParam mbti: String
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val detail = mbtiService.viewResult(mbti)
        return ResponseEntity.ok(ApiResult.success(detail))
    }
}
