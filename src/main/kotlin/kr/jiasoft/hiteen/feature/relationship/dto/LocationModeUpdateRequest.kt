package kr.jiasoft.hiteen.feature.relationship.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.relationship.domain.LocationMode

@Schema(description = "위치 모드 업데이트 요청")
data class UpdateLocationModeRequest(

    @param:Schema(description = "상대방 UID", example = "a1a8990f-2443-4492-baad-699d59b272fa")
    val userUid: String,      // 상대방 UID

    @param:Schema(description = "위치 모드", example = "PUBLIC")
    val mode: LocationMode      // PUBLIC / HIDDEN / RANDOM
)
