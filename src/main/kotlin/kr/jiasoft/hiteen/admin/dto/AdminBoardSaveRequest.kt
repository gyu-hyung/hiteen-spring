package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "관리자 게시글 등록/수정(배너 포함) 요청")
data class AdminBoardSaveRequest(
    @field:Schema(description = "수정 시 게시글 id", example = "1")
    val id: Long? = null,

    @field:Schema(description = "카테고리", example = "EVENT")
    val category: String,

    val subject: String? = null,
    val content: String,
    val link: String? = null,
    val ip: String? = null,

    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,

    @field:Schema(description = "게시글 상태", example = "ACTIVE")
    val status: String,

    val address: String? = null,
    val detailAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,

    @field:Schema(description = "EVENT/EVENT_WINNING일 때 사용할 큰 배너 uid 목록")
    val largeBanners: List<UUID>? = null,

    @field:Schema(description = "EVENT/EVENT_WINNING일 때 사용할 작은 배너 uid 목록")
    val smallBanners: List<UUID>? = null,

    @field:Schema(description = "(일반 게시글) 삭제할 첨부(board_assets) UID 목록")
    val deleteAssetUids: List<UUID>? = null,

    @field:Schema(description = "(일반 게시글) 첨부 전체 교체 여부")
    val replaceAssets: Boolean? = null,

    @field:Schema(description = "(EVENT 게시글) 배너 전체 교체 여부. true면 기존 배너 삭제 후 업로드/uid만으로 재구성")
    val replaceBanners: Boolean? = true,

    @field:Schema(description = "(EVENT 게시글) 삭제할 큰 배너 uid 목록")
    val deleteLargeBannerUids: List<UUID>? = null,

    @field:Schema(description = "(EVENT 게시글) 삭제할 작은 배너 uid 목록")
    val deleteSmallBannerUids: List<UUID>? = null,
)
