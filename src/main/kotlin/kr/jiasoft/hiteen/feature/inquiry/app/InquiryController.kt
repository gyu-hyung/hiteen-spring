package kr.jiasoft.hiteen.feature.inquiry.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.inquiry.domain.InquiryEntity
import kr.jiasoft.hiteen.feature.inquiry.domain.InquiryStatus
import kr.jiasoft.hiteen.feature.inquiry.dto.InquiryCreateRequest
import kr.jiasoft.hiteen.feature.inquiry.infra.InquiryRepository
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@Tag(name = "Inquiry", description = "문의하기 API (웹용)")
@RestController
@RequestMapping("/api/inquiry")
class InquiryController(
    private val inquiryRepository: InquiryRepository,
) {

    @Operation(summary = "문의하기 등록", description = "웹에서 문의하기를 등록합니다.")
    @PostMapping
    suspend fun create(
        @RequestBody req: InquiryCreateRequest,
        request: ServerHttpRequest,
    ): ResponseEntity<ApiResult<Map<String, Any>>> {
        val ip = request.remoteAddress?.address?.hostAddress

        val saved = inquiryRepository.save(
            InquiryEntity(
                name = req.name,
                phone = req.phone,
                email = req.email,
                content = req.content,
                ip = ip,
                status = InquiryStatus.PENDING.name,
                createdAt = OffsetDateTime.now(),
            )
        )

        return ResponseEntity.ok(ApiResult.success(mapOf("id" to saved.id)))
    }
}

