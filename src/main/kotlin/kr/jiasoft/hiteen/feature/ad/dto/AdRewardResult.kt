package kr.jiasoft.hiteen.feature.ad.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.ad.domain.AdmobRewardEntity

@Schema(description = "광고 보상 결과")
data class AdRewardResult(
    @field:Schema(description = "광고 보상")
    val reward: AdmobRewardEntity,

    @field:Schema(description = "남은 광고 보상 횟수")
    val remaining: Int
)
