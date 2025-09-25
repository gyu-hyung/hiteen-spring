package kr.jiasoft.hiteen.feature.level.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.level.domain.TierCode
import kr.jiasoft.hiteen.feature.level.domain.TierEntity
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tiers")
class TierController(
    private val tierRepository: TierRepository
) {

    @Schema(description = "티어 응답")
    data class TierResponse(
        @field:Schema(description = "티어 ID")
        val id: Long,
        @field:Schema(description = "티어 코드")
        val tierCode: String,
        @field:Schema(description = "티어 이름")
        val tierNameKr: String,
        @field:Schema(description = "티어 세부 순서")
        val divisionNo: Int,
        @field:Schema(description = "티어 순서")
        val rankOrder: Int,
        @field:Schema(description = "티어 상태")
        val status: String,
        @field:Schema(description = "티어 최소 포인트")
        val minPoints: Int,
        @field:Schema(description = "티어 최대 포인트")
        val maxPoints: Int,
        @field:Schema(description = "티어 레벨")
        val level: Int
    ) {
        companion object {
            fun fromEntity(entity: TierEntity): TierResponse =
                TierResponse(
                    id = entity.id,
                    tierCode = entity.tierCode,
                    tierNameKr = entity.tierNameKr,
                    divisionNo = entity.divisionNo,
                    rankOrder = entity.rankOrder,
                    status = entity.status,
                    minPoints = entity.minPoints,
                    maxPoints = entity.maxPoints,
                    level = TierCode.fromCode(entity.tierCode).level
                )
        }
    }


    @Operation(summary = "티어 목록 조회", description = "티어 목록을 조회합니다.")
    @GetMapping
    suspend fun getTiers(): ResponseEntity<ApiResult<List<TierResponse>>> {
        val tiers = tierRepository.findAllOrdered()
            .toList()
            .map { TierResponse.fromEntity(it) }

        return ResponseEntity.ok(ApiResult.success(tiers))
    }

}
