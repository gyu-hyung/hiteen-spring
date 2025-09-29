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

//@Service
//class ReportService(
//    private val reportRepository: ReportRepository,
//    private val userRepository: UserRepository,
//    private val userService: UserService
//) {
//
//    suspend fun createReport(userId: Long, userUid: UUID, req: ReportRequest): ReportResponse {
//        val targetId = userRepository.findIdByUid(req.targetUid)
//        val entity = ReportEntity(
//            userId = userId,
//            targetId = targetId,
//            type = req.type,
//            reportableType = req.reportableType,
//            reportableId = req.reportableId,
//            reason = req.reason,
//            photoUid = req.photoUid
//        )
//        val saved = reportRepository.save(entity)
//
//
//        // uid & summary 변환
//        val targetUid = saved.targetId?.let { userRepository.findUidById(it) }
//        val userSummary = saved.userId.let { userService.findUserSummary(it) }
//        val targetSummary = saved.targetId?.let { userService.findUserSummary(it) }
//
//        return saved.toResponse(userUid, targetUid, userSummary, targetSummary)
//    }
//
//    suspend fun getReportsByUser(userId: Long): List<ReportResponse> {
//        val userUid = userRepository.findUidById(userId)
//        val reports = reportRepository.findAllByUserId(userId)
//
//        return reports.map { report ->
//            val targetUid = report.targetId?.let { userRepository.findUidById(it) }
//            val userSummary = report.userId.let { userService.findUserSummary(it) }
//            val targetSummary = report.targetId?.let { userService.findUserSummary(it) }
//
//            report.toResponse(userUid!!, targetUid, userSummary, targetSummary)
//        }
//    }
//
//}


@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val boardRepository: BoardRepository,
    private val boardCommentRepository: BoardCommentRepository,
    private val pollCommentRepository: PollCommentRepository,
    private val userService: UserService
) {

    suspend fun createReport(userId: Long, userUid: UUID, req: ReportRequest): ReportResponse {
        val targetId = req.targetUid?.let { userRepository.findIdByUid(it) }

        // 신고 대상 컨텐츠 식별자 변환
        val (reportableType, reportableId) = when (req.type.uppercase()) {
            "BOARD" -> {
                val id = req.targetContentUid?.let { boardRepository.findIdByUid(it) }
                    ?: throw IllegalArgumentException("게시글 UID가 필요합니다.")
                "BOARD" to id
            }
            "COMMENT" -> {
                val id = req.targetContentUid?.let { boardCommentRepository.findIdByUid(it) }
                    ?: throw IllegalArgumentException("댓글 UID가 필요합니다.")
                "COMMENT" to id
            }
            "POLL" -> {
                val id = req.targetContentId
                    ?: throw IllegalArgumentException("투표 ID가 필요합니다.")
                "POLL" to id
            }
            "POLL_COMMENT" -> {
                val id = req.targetContentUid?.let { pollCommentRepository.findIdByUid(it) }
                    ?: throw IllegalArgumentException("투표 댓글 UID가 필요합니다.")
                "POLL_COMMENT" to id
            }
            else -> throw IllegalArgumentException("지원하지 않는 신고 타입입니다: ${req.type}")
        }

        val entity = ReportEntity(
            userId = userId,
            targetId = targetId,
            type = req.type.uppercase(),
            reportableType = reportableType,
            reportableId = reportableId,
            reason = req.reason,
            photoUid = req.photoUid
        )
        val saved = reportRepository.save(entity)

        // uid & summary 변환
        val targetUid = saved.targetId?.let { userRepository.findUidById(it) }
        val userSummary = userService.findUserSummary(saved.userId)
        val targetSummary = saved.targetId?.let { userService.findUserSummary(it) }

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
