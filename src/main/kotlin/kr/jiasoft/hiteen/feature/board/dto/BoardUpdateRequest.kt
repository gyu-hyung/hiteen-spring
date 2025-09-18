package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

@Schema(description = "게시글 수정 요청 DTO")
data class BoardUpdateRequest(

    @field:NotNull
    @param:Schema(
        description = "수정할 게시글 UID",
        example = "550e8400-e29b-41d4-a716-446655440000",
        required = true
    )
    val boardUid: UUID? = null,

    @param:Schema(
        description = "게시글 제목",
        example = "오늘 운동 같이 하실 분?"
    )
    val subject: String? = null,

    @param:Schema(
        description = "게시글 내용",
        example = "헬스장 같이 갈 사람 구해요. 시간은 오후 7시쯤!"
    )
    val content: String? = null,

    @param:Schema(
        description = "카테고리",
        example = "운동/헬스"
    )
    val category: String? = null,

    @param:Schema(
        description = "참고 링크",
        example = "https://example.com/post/123"
    )
    val link: String? = null,

    @param:Schema(
        description = "시작일 (게시 유효기간 시작)",
        example = "2025.09.18"
    )
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val startDate: LocalDate? = null,

    @param:Schema(
        description = "종료일 (게시 유효기간 종료)",
        example = "2025.09.30"
    )
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val endDate: LocalDate? = null,

    @param:Schema(
        description = "상태 (예: ACTIVE, INACTIVE, DELETED)",
        example = "ACTIVE"
    )
    val status: String? = null,

    @param:Schema(
        description = "주소",
        example = "서울특별시 강남구 테헤란로 123"
    )
    val address: String? = null,

    @param:Schema(
        description = "상세 주소",
        example = "아파트 101동 202호"
    )
    val detailAddress: String? = null,

    @param:Schema(
        description = "삭제할 첨부파일 UID 목록 (null = 변경 없음, [] = 모두 제거)",
        example = "[\"550e8400-e29b-41d4-a716-446655440001\", \"550e8400-e29b-41d4-a716-446655440002\"]"
    )
    val deleteAssetUids: List<UUID>? = null,

    @param:Schema(
        description = "첨부파일 전체 교체 여부 (true=새 첨부파일로 교체, false/미지정=기존 유지)",
        example = "true"
    )
    val replaceAssets: Boolean? = null,
)
