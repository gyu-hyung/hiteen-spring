package kr.jiasoft.hiteen.feature.play.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "시즌 랭킹 응답 DTO")
data class SeasonRankingResponse(

    @field:Schema(description = "랭킹 리스트")
    val rankings: List<RankingResponse>,

    @field:Schema(description = "내 랭킹")
    val myRanking: RankingResponse? // 내 랭킹 강조용
)