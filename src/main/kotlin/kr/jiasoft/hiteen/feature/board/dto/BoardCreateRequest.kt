package kr.jiasoft.hiteen.feature.board.dto

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import kr.jiasoft.hiteen.feature.board.app.BoardService
import kr.jiasoft.hiteen.feature.board.domain.BoardCategory
import java.time.LocalDate

@Schema(description = "게시판 글 등록 요청 DTO")
data class BoardCreateRequest(

//    @field:NotBlank
//    @field:Size(max = 200)
    @param:Schema(
        description = "게시글 제목",
        example = "오늘 저녁 같이 운동하실 분?",
        maxLength = 200,
        required = true
    )
    val subject: String?,

    @field:NotBlank
    @param:Schema(
        description = "게시글 내용",
        example = "저녁 7시에 광주 체육관 앞에서 모여요!",
        required = true
    )
    val content: String,

    @field:Size(max = 50)
    @param:Schema(
        description = "게시글 카테고리",
        example = "STORY / EVENT / NOTICE",
        maxLength = 50
    )
    val category: BoardCategory,

    @param:Schema(
        description = "참고 링크",
        example = "https://example.com"
    )
    val link: String? = null,

    @param:Schema(
        description = "게시글 유효 시작일",
        example = "2025.09.18"
    )
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val startDate: LocalDate? = null,

    @param:Schema(
        description = "게시글 유효 종료일",
        example = "2025.09.30"
    )
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
    val endDate: LocalDate? = null,

    @param:Schema(
        description = "게시글 상태",
        example = "ACTIVE",
        required = true
    )
    val status: String,

    @param:Schema(
        description = "주소 (시/군/구 단위)",
        example = "광주광역시 북구 시청로 1",
        required = true
    )
    val address: String? = null,

    @param:Schema(
        description = "상세 주소",
        example = "2층 강당"
    )
    val detailAddress: String? = null,
)
