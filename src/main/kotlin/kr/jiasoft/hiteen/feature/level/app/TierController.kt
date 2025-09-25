package kr.jiasoft.hiteen.feature.level.app

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

    data class TierResponse(
        val id: Long,
        val tierCode: String,
        val tierNameKr: String,
        val divisionNo: Int,
        val rankOrder: Int,
        val status: String,
        val minPoints: Int,
        val maxPoints: Int,
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


    @GetMapping
    suspend fun getTiers(): ResponseEntity<ApiResult<List<TierResponse>>> {
        val tiers = tierRepository.findAllOrdered()
            .toList()
            .map { TierResponse.fromEntity(it) }

        return ResponseEntity.ok(ApiResult.success(tiers))
    }

}
