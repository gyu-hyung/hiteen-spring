package kr.jiasoft.hiteen.feature.relationship.dto

import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import java.time.OffsetDateTime

data class RelationshipSummary(
    val userSummary: UserSummary,
    val status: String,          // PENDING / ACCEPTED / ...
    val statusAt: OffsetDateTime?,
    val lat: Double? = null,
    val lng: Double? = null,
    val lastSeenAt: Long? = null,
)