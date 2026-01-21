package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "라벨정보 등록/수정 요청")
data class AdminLevelSaveRequest(
    val mode: String? = "add",
    val id: Long? = null,
    val tierCode: String,
    val tierNameKr: String,

    val divisionNo: Int = 0,
    val level: Int = 0,
    val rankOrder: Int = 1,
    val status: String = "ACTIVE",

    val minPoints: Int = 0,
    val maxPoints: Int = 0,
)
