package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminSmsAuthResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsDetailResponse
import kr.jiasoft.hiteen.admin.dto.AdminSmsListResponse
import kr.jiasoft.hiteen.admin.infra.AdminSmsAuthRepository
import kr.jiasoft.hiteen.admin.infra.AdminSmsRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.sms.domain.SmsAuthEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import org.springframework.stereotype.Service

@Service
class AdminSmsService(
    private val adminSmsRepository: AdminSmsRepository,
    private val adminSmsAuthRepository: AdminSmsAuthRepository,
) {

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

