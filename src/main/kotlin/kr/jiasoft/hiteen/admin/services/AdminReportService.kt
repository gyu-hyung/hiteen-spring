package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminReportProcessRequest
import kr.jiasoft.hiteen.admin.dto.AdminReportRejectRequest
import kr.jiasoft.hiteen.admin.dto.AdminReportResponse
import kr.jiasoft.hiteen.admin.infra.AdminReportRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.report.infra.ReportRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdminReportService(
    private val adminReportRepository: AdminReportRepository,
    private val reportRepository: ReportRepository,
    private val expService: ExpService,
    private val txOperator: TransactionalOperator,
) {

    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        status: Int? = null,
        type: String? = null,
        userUid: UUID? = null,
        targetUid: UUID? = null,
    ) = adminReportRepository.listByPage(
        page = page,
        size = size,
        order = order,
        status = status,
        type = type,
        userUid = userUid,
        targetUid = targetUid,
    )

    suspend fun totalCount(
        status: Int? = null,
        type: String? = null,
        userUid: UUID? = null,
        targetUid: UUID? = null,
    ) = adminReportRepository.totalCount(
        status = status,
        type = type,
        userUid = userUid,
        targetUid = targetUid,
    )

    suspend fun detail(id: Long): AdminReportResponse {
        return adminReportRepository.findDetailById(id)
            ?: throw IllegalArgumentException("존재하지 않는 신고입니다. id=$id")
    }

    /**
     * 신고 처리(완료) + 경험치 감점(정책 기반)
     * - 사용자 신고 생성 시점에는 경험치 처리하지 않음
     * - 처리 완료 시점에만 조치 및 경험치 처리
     */
    suspend fun process(id: Long, request: AdminReportProcessRequest): AdminReportResponse =
        txOperator.executeAndAwait {
            val report = reportRepository.findById(id)
                ?: throw IllegalArgumentException("존재하지 않는 신고입니다. id=$id")

            // 이미 처리/반려된 신고면 막기
            if (report.status != 0) {
                throw IllegalStateException("이미 처리된 신고입니다. id=$id")
            }

            val updated = report.copy(
                status = 1,
                answer = request.answer,
                memo = request.memo,
                answerAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )

            reportRepository.save(updated)

            val targetId = report.targetId
                ?: throw IllegalArgumentException("신고 대상(targetId)이 없어 경험치 처리를 할 수 없습니다. id=$id")

            val dynamicMemo = when (report.reportableType) {
                "불쾌한 사진" -> "불쾌한 사진 신고로 인해 감점 처리 되었습니다."
                "허위 프로필" -> "허위 프로필 신고로 인해 감점 처리 되었습니다."
                "사진 도용" -> "사진 도용 신고로 인해 감점 처리 되었습니다."
                "욕설 및 비방" -> "욕설 및 비방 신고로 인해 감점 처리 되었습니다."
                "불법촬영물 공유" -> "불법촬영물 공유 신고로 인해 감점 처리 되었습니다."
                else -> "사용자의 신고에 의해 감점처리 되었습니다."
            }

            expService.grantExp(
                userId = targetId,
                actionCode = "REPORT",
                targetId = report.id,
                dynamicMemo = dynamicMemo,
            )

            adminReportRepository.findDetailById(id)
                ?: throw IllegalStateException("처리 후 데이터를 다시 조회할 수 없습니다. id=$id")
        }

    /**
     * 신고 반려
     * - status를 2로 변경
     * - 경험치 처리는 수행하지 않음
     */
    suspend fun reject(id: Long, request: AdminReportRejectRequest): AdminReportResponse =
        txOperator.executeAndAwait {
            val report = reportRepository.findById(id)
                ?: throw IllegalArgumentException("존재하지 않는 신고입니다. id=$id")

            if (report.status != 0) {
                throw IllegalStateException("이미 처리된 신고입니다. id=$id")
            }

            val updated = report.copy(
                status = 2,
                memo = request.memo,
                updatedAt = OffsetDateTime.now(),
            )

            reportRepository.save(updated)

            adminReportRepository.findDetailById(id)
                ?: throw IllegalStateException("반려 후 데이터를 다시 조회할 수 없습니다. id=$id")
        }
}
