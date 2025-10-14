package kr.jiasoft.hiteen.feature.study.app

import io.swagger.v3.oas.annotations.Operation
import kr.jiasoft.hiteen.feature.study.dto.StudyResponse
import kr.jiasoft.hiteen.feature.study.dto.StudyStartRequest
import kr.jiasoft.hiteen.feature.study.dto.StudyStartResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/study")
class StudyController(
    private val studyService: StudyService
) {

    @Operation(summary = "영어 단어 학습 시작", description = "시즌별·학습단계별 문제 세트를 조회합니다.")
    @PostMapping("/start")
    suspend fun startStudy(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        request: StudyStartRequest
    ): StudyStartResponse = studyService.startStudy(user, request)

    @Operation(summary = "영어 단어 학습 완료")
    @PostMapping("/complete/{uid}")
    suspend fun completeStudy(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable uid: String
    ): StudyResponse = studyService.completeStudy(user.id, uid)
}
