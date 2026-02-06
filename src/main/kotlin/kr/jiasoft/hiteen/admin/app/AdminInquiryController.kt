package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminInquiryReplyRequest
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.inquiry.domain.InquiryStatus
import kr.jiasoft.hiteen.feature.inquiry.dto.InquiryResponse
import kr.jiasoft.hiteen.feature.inquiry.infra.InquiryRepository
import kr.jiasoft.hiteen.feature.sms.app.SmsService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@Tag(name = "AdminInquiry", description = "관리자 문의하기 관리 API")
@RestController
@RequestMapping("/api/admin/inquiry")
@SecurityRequirement(name = "bearerAuth")
class AdminInquiryController(
    private val inquiryRepository: InquiryRepository,
    private val smsService: SmsService,
) {

    /**
     * 문의 목록 조회 (페이지 기반)
     */
    @Operation(summary = "문의 목록 조회", description = "관리자용 문의 목록을 페이지 기반으로 조회합니다.")
    @GetMapping
    suspend fun listByPage(
        @Parameter(description = "페이지 번호 (1부터 시작)") @RequestParam page: Int = 1,
        @Parameter(description = "페이지당 개수") @RequestParam size: Int = 10,
        @Parameter(description = "정렬 순서 (ASC/DESC)") @RequestParam order: String = "DESC",
        @Parameter(description = "상태 (PENDING/REPLIED/CLOSED/ALL)") @RequestParam status: String? = null,
        @Parameter(description = "검색어 (이름, 전화번호, 이메일, 내용)") @RequestParam search: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<InquiryResponse>>> {

        val safePage = page.coerceAtLeast(1)
        val safeSize = size.coerceIn(1, 100)

        val list = inquiryRepository.listByPage(
            page = safePage,
            size = safeSize,
            order = order,
            status = status,
            search = search,
        ).toList()

        val mapped = list.map { entity ->
            InquiryResponse(
                id = entity.id,
                name = entity.name,
                phone = entity.phone,
                email = entity.email,
                content = entity.content,
                status = entity.status,
                replyContent = entity.replyContent,
                replyAt = entity.replyAt,
                createdAt = entity.createdAt,
            )
        }

        val totalCount = inquiryRepository.totalCount(status, search)

        return ResponseEntity.ok(
            ApiResult.success(PageUtil.of(mapped, totalCount, safePage, safeSize))
        )
    }

    /**
     * 문의 단건 조회
     */
    @Operation(summary = "문의 단건 조회", description = "관리자용 문의 단건을 조회합니다.")
    @GetMapping("/{id}")
    suspend fun getById(
        @Parameter(description = "문의 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<InquiryResponse>> {
        val entity = inquiryRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 문의입니다. id=$id")

        val response = InquiryResponse(
            id = entity.id,
            name = entity.name,
            phone = entity.phone,
            email = entity.email,
            content = entity.content,
            status = entity.status,
            replyContent = entity.replyContent,
            replyAt = entity.replyAt,
            createdAt = entity.createdAt,
        )

        return ResponseEntity.ok(ApiResult.success(response))
    }

    /**
     * 문의 답변 및 SMS 발송
     */
    @Operation(summary = "문의 답변 및 SMS 발송", description = "문의에 답변하고 선택적으로 SMS를 발송합니다.")
    @PostMapping("/{id}/reply")
    suspend fun reply(
        @Parameter(description = "문의 ID") @PathVariable id: Long,
        @RequestBody req: AdminInquiryReplyRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val entity = inquiryRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 문의입니다. id=$id")

        // 답변 저장
        val updated = entity.copy(
            replyContent = req.replyContent,
            replyAt = OffsetDateTime.now(),
            replyBy = user.id,
            status = InquiryStatus.REPLIED.name,
            updatedAt = OffsetDateTime.now(),
        )
        inquiryRepository.save(updated)

        // SMS 발송 (선택적)
        var smsSent = false
        if (req.sendSms) {
            val smsMessage = req.smsMessage ?: "[하이틴] 문의하신 내용에 대해 답변드립니다.\n\n${req.replyContent}"
            smsSent = smsService.sendSms(null, entity.phone, smsMessage)
        }

        return ResponseEntity.ok(
            ApiResult.success(
                mapOf(
                    "id" to id,
                    "replied" to true,
                    "smsSent" to smsSent
                )
            )
        )
    }

    /**
     * 문의 상태 변경
     */
    @Operation(summary = "문의 상태 변경", description = "문의 상태를 변경합니다.")
    @PostMapping("/{id}/status")
    suspend fun changeStatus(
        @Parameter(description = "문의 ID") @PathVariable id: Long,
        @Parameter(description = "변경할 상태") @RequestParam status: InquiryStatus,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        val entity = inquiryRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 문의입니다. id=$id")

        val updated = entity.copy(
            status = status.name,
            updatedAt = OffsetDateTime.now(),
        )
        inquiryRepository.save(updated)

        return ResponseEntity.ok(ApiResult.success(Unit))
    }

    /**
     * 문의 삭제 (Hard Delete)
     */
    @Operation(summary = "문의 삭제", description = "문의를 삭제합니다.")
    @DeleteMapping("/{id}")
    suspend fun delete(
        @Parameter(description = "문의 ID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Unit>> {
        val entity = inquiryRepository.findById(id)
            ?: throw IllegalArgumentException("존재하지 않는 문의입니다. id=$id")

        inquiryRepository.delete(entity)

        return ResponseEntity.ok(ApiResult.success(Unit))
    }
}

