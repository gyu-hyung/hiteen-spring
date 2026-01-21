package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*
import java.time.LocalDate

@Schema(description = "학교정보 등록/수정 요청")
data class AdminSchoolSaveRequest(
    val id: Long? = null,

    @param:Schema(description = "add | edit")
    val mode: String? = "add",

    @field:NotBlank(message = "학교명을 입력해 주세요.")
    val name: String,

    @field:NotBlank(message = "시/도를 선택해 주세요.")
    val sido: String,

    val sidoName: String? = null,

    @field:Min(value = 1, message = "학교구분을 선택해 주세요.")
    val type: Int = 1,

    val typeName: String? = null,

    val zipcode: String? = null,

    @field:NotBlank(message = "주소를 입력해 주세요.")
    val address: String? = null,

    val foundDate: LocalDate? = null,

    @field:NotNull(message = "학교 위치를 선택해 주세요.")
    @field:DecimalMin(value = "0.0000001", inclusive = true, message = "학교 위치를 선택해 주세요.")
    @field:DecimalMax(value = "90.0", inclusive = true, message = "학교 위치를 선택해 주세요.")
    val latitude: Double?,

    @field:NotNull(message = "학교 위치를 선택해 주세요.")
    @field:DecimalMin(value = "0.0000001", inclusive = true, message = "학교 위치를 선택해 주세요.")
    @field:DecimalMax(value = "180.0", inclusive = true, message = "학교 위치를 선택해 주세요.")
    val longitude: Double?,
)
