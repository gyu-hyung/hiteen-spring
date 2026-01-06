package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "관리자 게시글 등록/수정 요청")
data class AdminBoardCreateRequest(
    val id: Long? = null,

    val category: String,
    val subject: String?,
    val content: String,
    val link: String? = null,

    val ip: String? = null,

    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,

    val status: String,

    val address: String? = null,
    val detailAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)
