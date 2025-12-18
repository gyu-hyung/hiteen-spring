package kr.jiasoft.hiteen.challengeRewardPolicy.dto

import java.util.UUID

data class ChallengeRewardPolicyItemDto(
    val id: Long?,          // null = 신규
    val type: String,
    val league: String?,
    val gameId: Long?,
    val amount: Int?,
    val goodsCodes: String?,
    val rank: Int?,
    val message: String?,
    val memo: String?,
    val status: Short,
    val orderNo: Int,
    val assetUid: UUID?
)
