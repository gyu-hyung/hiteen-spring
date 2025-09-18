package kr.jiasoft.hiteen.feature.board.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class BoardUpdateRequest(
    @field:NotNull
    val boardUid: UUID? = null,
    val subject: String? = null,
    val content: String? = null,
    val category: String? = null,
    val link: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String? = null,
    val address: String? = null,
    val detailAddress: String? = null,
    val deleteAssetUids: List<UUID>? = null, // null=변경없음, []=모두제거
    val replaceAssets: Boolean? = null,
)