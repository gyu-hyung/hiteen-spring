package kr.jiasoft.hiteen.admin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "관리자 공지사항/이벤트 등록/수정 요청")
data class AdminArticleSaveRequest(
    @field:Schema(description = "수정 시 게시글 id", example = "1")
    val id: Long? = null,

    @field:Schema(description = "카테고리 (NOTICE / EVENT)", example = "NOTICE")
    val category: String,

    @field:Schema(description = "제목", example = "긴급 공지사항")
    val subject: String? = null,

    @field:Schema(description = "내용", example = "서버 점검이 예정되어 있습니다.")
    val content: String,

    @field:Schema(description = "외부 링크", example = "https://example.com")
    val link: String? = null,

    @field:Schema(description = "작성자 IP")
    val ip: String? = null,

    @field:Schema(description = "시작일", example = "2025-01-01")
    val startDate: LocalDate? = null,

    @field:Schema(description = "종료일", example = "2025-12-31")
    val endDate: LocalDate? = null,

    @field:Schema(description = "상태 (ACTIVE: 진행중, INACTIVE: 비활성, ENDED: 종료, WINNING: 당첨자발표)", example = "ACTIVE")
    val status: String,

    // 일반 첨부파일 (NOTICE용)
    @field:Schema(description = "첨부파일 UID 목록 (이미 업로드된 파일)")
    val assetUids: List<UUID>? = null,

    @field:Schema(description = "삭제할 첨부파일 UID 목록")
    val deleteAssetUids: List<UUID>? = null,

    @field:Schema(description = "첨부파일 전체 교체 여부. true면 기존 첨부 삭제 후 재구성")
    val replaceAssets: Boolean? = false,

    // 이벤트 배너 (EVENT용)
    @field:Schema(description = "(EVENT) 큰 배너 UID 목록")
    val largeBanners: List<UUID>? = null,

    @field:Schema(description = "(EVENT) 작은 배너 UID 목록")
    val smallBanners: List<UUID>? = null,

    @field:Schema(description = "(EVENT) 배너 전체 교체 여부. true면 기존 배너 삭제 후 재구성")
    val replaceBanners: Boolean? = true,

    @field:Schema(description = "(EVENT) 삭제할 큰 배너 UID 목록")
    val deleteLargeBannerUids: List<UUID>? = null,

    @field:Schema(description = "(EVENT) 삭제할 작은 배너 UID 목록")
    val deleteSmallBannerUids: List<UUID>? = null,
)

