package kr.jiasoft.hiteen.feature.relationship.dto

import java.time.OffsetDateTime

data class FollowSummary(
    val uid: String,
    val username: String,
    val nickname: String?,
    val telno: String?,
    val address: String?,
    val detailAddress: String?,
    val mood: String?,
    val tier: String?,
    val assetUid: String?,
    val status: String,          // PENDING / ACCEPTED / ...
    val statusAt: OffsetDateTime?
)