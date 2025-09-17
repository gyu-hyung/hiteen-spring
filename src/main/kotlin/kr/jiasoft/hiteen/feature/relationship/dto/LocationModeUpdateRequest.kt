package kr.jiasoft.hiteen.feature.relationship.dto

import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode

data class UpdateLocationModeRequest(
    val friendUid: String,      // 상대방 UID
    val mode: LocationMode      // PUBLIC / HIDDEN / RANDOM
)
