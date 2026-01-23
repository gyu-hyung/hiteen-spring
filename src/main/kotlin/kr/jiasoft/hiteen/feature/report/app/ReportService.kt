package kr.jiasoft.hiteen.feature.report.app

import kr.jiasoft.hiteen.feature.board.infra.BoardCommentRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
import kr.jiasoft.hiteen.feature.poll.infra.PollCommentRepository
import kr.jiasoft.hiteen.feature.report.domain.ReportEntity
import kr.jiasoft.hiteen.feature.report.infra.ReportRepository
import kr.jiasoft.hiteen.feature.report.dto.ReportRequest
import kr.jiasoft.hiteen.feature.report.dto.ReportResponse
import kr.jiasoft.hiteen.feature.report.domain.toResponse
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val boardRepository: BoardRepository,
    private val boardCommentRepository: BoardCommentRepository,
    private val pollCommentRepository: PollCommentRepository,
    private val userService: UserService,
) {

    suspend fun createReport(userId: Long, userUid: UUID, req: ReportRequest): ReportResponse {
        val targetId = req.targetUid?.let { userRepository.findIdByUid(it) }

        // ✅ reportableType(신고 분류) 검증
        // - 클라이언트에서 넘어오는 값은 한글 문구(예: "불쾌한 사진")로 정의되어 있음
        // - 기존 구현에서 null/임의값도 저장 가능했으나, 운영상 허용된 값만 저장되게 제한
        val allowedReportableTypes = setOf(
            "불쾌한 사진",
            "허위 프로필",
            "사진 도용",
            "욕설 및 비방",
            "불법촬영물 공유",
            "기타",
        )
        val normalizedReportableType = req.reportableType.trim()
        if (normalizedReportableType.isBlank() || normalizedReportableType !in allowedReportableTypes) {
            throw IllegalArgumentException(
                "신고 사유(reportableType)가 올바르지 않습니다. 허용값=${allowedReportableTypes.joinToString(", ")}."
            )
        }

        // 신고 대상 컨텐츠 식별자 변환
        val reportableId = when (req.type.uppercase()) {
            "BOARD" -> {
                val id = req.targetContentUid?.let { boardRepository.findIdByUid(it) }
                    ?: throw IllegalArgumentException("게시글 UID가 필요합니다.")
                id
            }
            "COMMENT" -> {
                val id = req.targetContentUid?.let { boardCommentRepository.findIdByUid(it) }
                    ?: throw IllegalArgumentException("댓글 UID가 필요합니다.")
                id
            }
            "POLL" -> {
                val id = req.targetContentId
                    ?: throw IllegalArgumentException("투표 ID가 필요합니다.")
                id
            }
            "POLL_COMMENT" -> {
                val id = req.targetContentUid?.let { pollCommentRepository.findIdByUid(it) }
                    ?: throw IllegalArgumentException("투표 댓글 UID가 필요합니다.")
                id
            }
            "TEEN_PICK" -> {
                val id = req.targetUid?.let { userRepository.findIdByUid(it) }
                    ?: throw IllegalArgumentException("회원 UID가 필요합니다.")
                id
            }
            else -> throw IllegalArgumentException("지원하지 않는 신고 타입입니다: ${req.type}")
        }

        val entity = ReportEntity(
            userId = userId,
            targetId = targetId,
            type = req.type.uppercase(),
            reportableType = normalizedReportableType,
            reportableId = reportableId,
            reason = req.reason,
            photoUid = req.photoUid
        )
        val saved = reportRepository.save(entity)

        // uid & summary 변환
        val targetUid = saved.targetId?.let { userRepository.findUidById(it) }
        val userSummary = userService.findUserSummary(saved.userId)
        val targetSummary = saved.targetId?.let { userService.findUserSummary(it) }

        // ✅ 경험치/포인트 처리는 관리자 처리 시점으로 이동

        return saved.toResponse(userUid, targetUid, userSummary, targetSummary)
    }

    suspend fun getReportsByUser(userId: Long): List<ReportResponse> {
        val userUid = userRepository.findUidById(userId)
        val reports = reportRepository.findAllByUserId(userId)

        return reports.map { report ->
            val targetUid = report.targetId?.let { userRepository.findUidById(it) }
            val userSummary = userService.findUserSummary(report.userId)
            val targetSummary = report.targetId?.let { userService.findUserSummary(it) }

            report.toResponse(userUid!!, targetUid, userSummary, targetSummary)
        }
    }
}
