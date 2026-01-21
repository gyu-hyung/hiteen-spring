package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminSmsAuthResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsDetailResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsDetailLogResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsListResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsSendRequest
import kr.jiasoft.hiteen.admin.dto.AdminSmsSendResponse
import kr.jiasoft.hiteen.admin.infra.AdminSmsAuthRepository
import kr.jiasoft.hiteen.admin.infra.AdminSmsDetailRepository
import kr.jiasoft.hiteen.admin.infra.AdminSmsRepository
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.sms.app.SmsService
import kr.jiasoft.hiteen.feature.sms.domain.SmsAuthEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsDetailEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class AdminSmsService(
    private val adminSmsRepository: AdminSmsRepository,
    private val adminSmsAuthRepository: AdminSmsAuthRepository,
    private val adminSmsDetailRepository: AdminSmsDetailRepository,
    private val smsService: SmsService,
    private val smsProperties: SmsProperties,
    private val adminUserRepository: AdminUserRepository,
) {

    @Transactional
    suspend fun sendSms(createdId: Long?, request: AdminSmsSendRequest): AdminSmsSendResponse {
        val hasAllSentinel = request.phones.any { it.equals("all", ignoreCase = true) }

        // 전화번호 정규화(숫자만) + 중복 제거
        val phones = if (request.sendAll || hasAllSentinel) {
            adminUserRepository.findByRole("USER")
                .asSequence()
                .map { it.phone }
                .filter { it.isNotBlank() }
                .map { it.filter(Char::isDigit) }
                .distinct()
                .toList()
        } else {
            request.phones
                .asSequence()
                .map { it.filter(Char::isDigit) }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
        }

        require(phones.isNotEmpty()) { "phones is required" }

        val now = OffsetDateTime.now()

        // sms row 먼저 생성(총발송수/콘텐츠 기록)
        val sms = adminSmsRepository.save(
            SmsEntity(
                title = request.title,
                content = request.content,
                callback = request.callback ?: smsProperties.callback,
                total = phones.size.toLong(),
                success = 0,
                failure = 0,
                createdId = createdId,
                createdAt = now,
                updatedAt = null,
            )
        )

        var successCount = 0L
        var failureCount = 0L

        // 알리고는 receiver 콤마로 동시 1000명 발송 지원
        // 1000명 단위로 쪼개서 bulk 호출 후 결과를 합산
        val batchSize = 1000
        phones.chunked(batchSize).forEach { batch ->
            val result = smsService.sendSmsBulk(
                sms = sms,
                phones = batch,
                message = request.content,
                title = request.title,
            )
            successCount += result.success.toLong()
            failureCount += result.failure.toLong()
        }

        // 실패 번호를 알리고가 응답에 내려주지 않아서(기본은 count만 제공), failedPhones는 제공하지 않음.
        val failedPhones: List<String> = emptyList()

        // 혹시 provider 응답과 대상 수가 안 맞는 케이스 방어
        if (successCount + failureCount > phones.size.toLong()) {
            failureCount = (phones.size.toLong() - successCount).coerceAtLeast(0)
        }

        // 최종 집계 반영
        adminSmsRepository.save(
            sms.copy(
                total = phones.size.toLong(),
                success = successCount,
                failure = failureCount,
                updatedAt = OffsetDateTime.now(),
            )
        )

        return AdminSmsSendResponse(
            smsId = sms.id,
            total = phones.size.toLong(),
            success = successCount,
            failure = failureCount,
            failedPhones = failedPhones,
            createdAt = sms.createdAt,
        )
    }

    suspend fun list(
        page: Int,
        size: Int,
        order: String,
        searchType: String,
        search: String?,
    ): ApiPage<AdminSmsListResponse> {
        val p = page.coerceAtLeast(1)
        val s = size.coerceIn(1, 100)
        val offset = (p - 1) * s

        val normalizedSearch = search?.trim()?.takeIf { it.isNotBlank() }
        val normalizedSearchType = when (searchType.trim().uppercase()) {
            "ALL", "PHONE", "CONTENT", "TITLE" -> searchType.trim().uppercase()
            else -> "ALL"
        }
        val normalizedOrder = when (order.trim().uppercase()) {
            "ASC" -> "ASC"
            else -> "DESC"
        }

        val total = adminSmsRepository.countList(normalizedSearch, normalizedSearchType)
        val rows = adminSmsRepository.list(normalizedSearch, normalizedSearchType, normalizedOrder, s, offset)

        return PageUtil.of(
            items = rows.map { it.toResponse() },
            total = total,
            page = p,
            size = s,
        )
    }

    suspend fun getSmsWithAuthLogs(
        smsId: Long,
        authPage: Int,
        authSize: Int,
        status: String?,
        includeDeleted: Boolean,
    ): AdminSmsDetailResponse {
        val sms = adminSmsRepository.findById(smsId)
            ?: throw IllegalStateException("sms not found: $smsId")

        val p = authPage.coerceAtLeast(1)
        val s = authSize.coerceIn(1, 200)
        val offset = (p - 1) * s

        val normalizedStatus = when (status?.trim()?.uppercase()) {
            null, "", "ALL" -> "ALL"
            else -> status.trim().uppercase()
        }

        val authLogs = adminSmsAuthRepository.listBySmsId(
            smsId = smsId,
            includeDeleted = includeDeleted,
            status = normalizedStatus,
            limit = s,
            offset = offset,
        )

        // 인증문자가 아니면 sms_auth가 비어있을 수 있으므로, sms_details로 대체 조회
        val smsDetails = if (authLogs.isEmpty()) {
            adminSmsDetailRepository.listBySmsId(
                smsId = smsId,
                includeDeleted = includeDeleted,
                limit = s,
                offset = offset,
            ).map { it.toDetailResponse() }
        } else {
            emptyList()
        }

        return AdminSmsDetailResponse(
            sms = sms.toResponse(),
            authLogs = authLogs.map { it.toResponse() },
            smsDetails = smsDetails,
        )
    }

    private fun SmsEntity.toResponse(): AdminSmsListResponse = AdminSmsListResponse(
        id = id,
        title = title,
        content = content,
        callback = callback,
        total = total,
        success = success,
        failure = failure,
        createdId = createdId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun SmsAuthEntity.toResponse(): AdminSmsAuthResponse = AdminSmsAuthResponse(
        id = id,
        smsId = smsId,
        phone = phone,
        code = code,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    private fun SmsDetailEntity.toDetailResponse(): AdminSmsDetailLogResponse = AdminSmsDetailLogResponse(
        id = id,
        smsId = smsId,
        phone = phone,
        success = success,
        error = error,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
