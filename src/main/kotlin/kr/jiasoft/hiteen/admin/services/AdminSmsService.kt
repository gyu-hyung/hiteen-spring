package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminSmsAuthResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsDetailResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsListResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsSendRequest
import kr.jiasoft.hiteen.admin.dto.AdminSmsSendResponse
import kr.jiasoft.hiteen.admin.infra.AdminSmsAuthRepository
import kr.jiasoft.hiteen.admin.infra.AdminSmsRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.sms.app.SmsService
import kr.jiasoft.hiteen.feature.sms.domain.SmsAuthEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class AdminSmsService(
    private val adminSmsRepository: AdminSmsRepository,
    private val adminSmsAuthRepository: AdminSmsAuthRepository,
    private val smsService: SmsService,
    private val smsProperties: SmsProperties,
) {

    @Transactional
    suspend fun sendSms(createdId: Long?, request: AdminSmsSendRequest): AdminSmsSendResponse {
        // 전화번호 정규화(숫자만) + 중복 제거
        val phones = request.phones
            .asSequence()
            .map { it.filter(Char::isDigit) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

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
        val failed = mutableListOf<String>()

        // SmsService가 sender는 props.callback 고정이라, callback을 바꾸려면 SmsService 개선이 필요.
        // 여기서는 isssue 없도록 sms_entity.callback만 기록하고 실제 전송 sender는 설정값을 사용.
        for (phone in phones) {
            val ok = smsService.sendSms(sms, phone, request.content)
            if (ok) successCount++ else failed.add(phone)
        }

        val failureCount = phones.size.toLong() - successCount

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
            failedPhones = failed,
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

        return AdminSmsDetailResponse(
            sms = sms.toResponse(),
            authLogs = authLogs.map { it.toResponse() },
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
}
