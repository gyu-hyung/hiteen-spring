package kr.jiasoft.hiteen.feature.banner.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "배너 DTO")
data class BannerResponse(
    @param:Schema(description = "배너 PK ", example = "1", hidden = true)
    val id: Long,

    @param:Schema(description = "배너 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID?,

    @param:Schema(description = "카테고리 (MAIN,AD)", example = "MAIN")
    val category: String,

    @param:Schema(description = "배너명", example = "회원가입 이벤트")
    val title: String? = null,

    @param:Schema(description = "링크유형(NONE,POPUP,BOARD,LINK)", example = "POPUP")
    val linkType: String? = null,

    @param:Schema(description = "링크 URL", example = "https://hiteen.kr")
    val linkURL: String? = null,

    @param:Schema(description = "게시판 코드(NOTICE/EVENT), 링크유형이 BOARD 인 경우", example = "EVENT")
    val bbsCode: String? = null,

    @param:Schema(description = "게시물(공지/이벤트) 번호, 링크유형이 BOARD 인 경우", example = "125")
    val bbsId: Int? = null,

    @param:Schema(description = "배너 이미지 UID", example = "550e8400-e29b-41d4-a716-446655441111")
    val assetUid: UUID? = null,

    @param:Schema(description = "배너 노출 시작일", example = "2026-01-01")
    val startDate: LocalDate? = null,

    @param:Schema(description = "배너 노출 종료일", example = "2026-01-30")
    val endDate: LocalDate? = null,

    @param:Schema(description = "배너 노출 여부", example = "ACTIVE")
    val status: String,

    @param:Schema(description = "등록일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,
)

