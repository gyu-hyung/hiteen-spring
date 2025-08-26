package kr.jiasoft.hiteen.feature.board.dto

import java.time.OffsetDateTime
import java.util.UUID

data class BoardUpdateRequest(
    val subject: String? = null,
    val content: String? = null,
    val category: String? = null,
    val link: String? = null,
    val startDate: OffsetDateTime? = null,
    val endDate: OffsetDateTime? = null,
    val status: String? = null,
    val address: String? = null,
    val detailAddress: String? = null,
    val deleteAssetUids: List<UUID>? = null, // null=변경없음, []=모두제거
    val replaceAssets: Boolean? = null,
)