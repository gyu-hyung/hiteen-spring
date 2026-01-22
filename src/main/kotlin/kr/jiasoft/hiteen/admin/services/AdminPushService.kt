package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminPushCreateRequest
import kr.jiasoft.hiteen.admin.dto.AdminPushDeleteResponse
import kr.jiasoft.hiteen.admin.dto.AdminPushDetailItem
import kr.jiasoft.hiteen.admin.dto.AdminPushDetailResponse
import kr.jiasoft.hiteen.admin.dto.AdminPushListResponse
import kr.jiasoft.hiteen.admin.infra.AdminPushDetailRepository
import kr.jiasoft.hiteen.admin.infra.AdminPushRepository
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushEntity
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AdminPushService(
    private val adminPushRepository: AdminPushRepository,
    private val adminPushDetailRepository: AdminPushDetailRepository,
    private val adminUserRepository: AdminUserRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    suspend fun list(
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        page: Int,
        perPage: Int,
        order: String,
    ): ApiPage<AdminPushListResponse> {
        val page = page.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage

        val search = search?.trim()?.takeIf { it.isNotBlank() }
        val order = when (order.trim().uppercase()) {
            "ASC" -> "ASC"
            else -> "DESC"
        }

        val total = adminPushRepository.countList(type, startDate, endDate, searchType, search)
        val rows = adminPushRepository.list(type, startDate, endDate, searchType, search, order, perPage, offset)

        return PageUtil.of(
            items = rows.map { it.toListResponse() },
            total = total,
            page = page,
            size = perPage,
        )
    }

    suspend fun get(pushId: Long, detailPage: Int, detailSize: Int, success: String?): AdminPushDetailResponse {
        val push = adminPushRepository.findById(pushId)
            ?: throw IllegalStateException("push not found: $pushId")

        val p = detailPage.coerceAtLeast(1)
        val s = detailSize.coerceIn(1, 200)
        val offset = (p - 1) * s

        val normalizedSuccess = when (success?.trim()?.uppercase()) {
            null, "", "ALL" -> "ALL"
            "SUCCESS" -> "SUCCESS"
            "FAIL" -> "FAIL"
            else -> "ALL"
        }

        val details = adminPushDetailRepository.listByPushId(pushId, normalizedSuccess, s, offset)

        return AdminPushDetailResponse(
            push = push.toListResponse(),
            details = details.map { d ->
                AdminPushDetailItem(
                    id = d.id,
                    pushId = d.pushId,
                    userId = d.userId,
                    deviceOs = d.deviceOs,
                    deviceToken = d.deviceToken,
                    phone = d.phone,
                    messageId = d.messageId,
                    error = d.error,
                    success = d.success,
                    createdAt = d.createdAt,
                )
            },
        )
    }

    @Transactional
    suspend fun createAndSend(createdId: Long?, request: AdminPushCreateRequest): AdminPushListResponse {
        val templateData = mutableMapOf<String, Any>()

        // 관리자 발송은 PushTemplate.ADMIN_SEND로 타입(code)을 고정
        templateData["code"] = PushTemplate.ADMIN_SEND.code
        // title/message는 관리자 요청값으로 덮어씀
        request.title?.let { templateData["title"] = it }
        request.message?.let { templateData["message"] = it }
        templateData["silent"] = (request.type?.lowercase() == "silent")

        val targetUserIds: List<Long> = if (request.sendAll) {
            adminUserRepository.findByRole("USER").map { it.id }
        } else {
            request.userIds
        }

        if (targetUserIds.isEmpty()) {
            throw IllegalArgumentException("회원을 한 명 이상 선택해 주세요.")
        }

        // PushService 내부에서 토큰 500개 단위 chunk로 멀티캐스트 발송/상세로그 저장을 처리하므로,
        // 여기서는 이벤트로 분리해 API 응답을 블로킹하지 않도록 한다.
        eventPublisher.publishEvent(
            PushSendRequestedEvent(
                userIds = targetUserIds,
                actorUserId = createdId,
                templateData = templateData,
            )
        )

        // 방금 저장된 pushId를 PushService에서 반환하지 않기 때문에,
        // 관리용 API에서는 최신 1건을 다시 조회(동시성 고려 필요). 여기서는 createdId 기준으로 가장 최근 push 1건을 가져옴.
        val latest = adminPushRepository.list(
            type = null,
            startDate = null,
            endDate = null,
            searchType = "ALL",
            search = null,
            order = "DESC",
            limit = 1,
            offset = 0,
        ).firstOrNull() ?: throw IllegalStateException("push create failed")

        return latest.toListResponse()
    }

    @Transactional
    suspend fun delete(pushId: Long): AdminPushDeleteResponse {
        val updated = adminPushRepository.softDelete(pushId)
        if (updated == 0) {
            val exists = adminPushRepository.findById(pushId)
            if (exists == null) throw IllegalStateException("push not found: $pushId")
        }
        return AdminPushDeleteResponse(id = pushId, deleted = true)
    }

    private fun PushEntity.toListResponse(): AdminPushListResponse = AdminPushListResponse(
        id = id,
        type = type,
        code = code,
        title = title,
        message = message,
        total = total,
        success = success,
        failure = failure,
        createdId = createdId,
        createdAt = createdAt,
        deletedAt = deletedAt,
    )
}
