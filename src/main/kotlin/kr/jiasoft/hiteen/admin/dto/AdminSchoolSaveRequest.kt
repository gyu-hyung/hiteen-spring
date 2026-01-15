package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "학교정보 등록/수정 요청")
data class AdminSchoolSaveRequest(
    val id: Long? = null,
    val mode: String? = "add",
    val name: String? = null,

    val sido: String? = "B10",
    val sidoName: String? = null,

    val type: Int = 1,
    val typeName: String? = null,

    val zipcode: String? = null,
    val address: String? = null,

    val foundDate: LocalDate? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,
)
