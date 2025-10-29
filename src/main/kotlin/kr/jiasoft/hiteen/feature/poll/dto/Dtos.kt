package kr.jiasoft.hiteen.feature.poll.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "투표 생성 요청 DTO")
data class PollCreateRequest(

    @param:Schema(description = "질문")
    val question: String,

    @param:Schema(description = "답변 목록")
    val answers: List<String>,

    @param:Schema(description = "시작 색상")
    val colorStart: String? = null,

    @param:Schema(description = "끝 색상")
    val colorEnd: String? = null,

    @param:Schema(description = "댓글 허용 여부 (0=불가,1=가능)")
    val allowComment: Int

    //    val onlyComment: Int = 0        // 1=댓글로 투표, 0=일반투표
)


@Schema(description = "투표 수정 요청 DTO")
data class PollUpdateRequest(

    @param:Schema(description = "투표 ID", example = "101")
    val id: Long,

    @param:Schema(description = "질문 내용", example = "올해 가장 기대되는 영화는 무엇인가요?")
    val question: String?,

    @param:Schema(
        description = "선택지 목록 (수정 시 교체됨)",
        example = "[\"듄 2\", \"데드풀 3\", \"베놈 3\"]"
    )
    val answers: List<String>?,

    @param:Schema(description = "시작 색상 HEX 코드", example = "#FF5733")
    val colorStart: String?,

    @param:Schema(description = "끝 색상 HEX 코드", example = "#FFC300")
    val colorEnd: String?,

    @param:Schema(description = "댓글 허용 여부", example = "true")
    val allowComment: Boolean?,
)


@Schema(description = "투표하기 요청 DTO")
data class PollVoteRequest(

    @param:Schema(description = "투표 ID", example = "1")
    val pollId: Long,

    @param:Schema(description = "선택지", example = "1")
    val seq: Int
)


@Schema(description = "투표 항목 결과")
data class PollSelectResponse(

    @param:Schema(description = "투표 선택지 ID", example = "1")
    val id: Long,

    @param:Schema(description = "번호", example = "1")
    val seq: Int,

    @param:Schema(description = "선택", example = "선택항목 1")
    val content: String,

    @param:Schema(description = "투표수", example = "999")
    val voteCount: Int,

    @param:Schema(description = "투표수", example = "999")
    val photos: List<String> = emptyList(),
)


data class VoteCountRow(
    val seq: Int,
    val votes: Long
)


data class PollSummaryRow(
    val id: Long,
    val question: String,
    val photo: String?,
    val selects: String?, // jsonb -> String
    val colorStart: String?,
    val colorEnd: String?,
    val voteCount: Int = 0,
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val likedByMe: Boolean = false,
    val votedByMe: Boolean = false,
    val votedSeq: Int? = null,
    val allowComment: Int = 0,
    val createdId: Long,
    val createdAt: OffsetDateTime,
)


@Schema(description = "투표 응답 정보")
data class PollResponse(

    @param:Schema(description = "투표 ID", example = "1")
    val id: Long,

    @param:Schema(description = "질문", example = "오늘 점심 뭐 먹을래?")
    val question: String,

    @param:Schema(description = "사진 Uid", example = "")
    val photos: List<String>? = emptyList(),

    @param:Schema(description = "선택지 목록")
    val selects: List<PollSelectResponse> = emptyList(),

    @param:Schema(description = "배경 시작 색상", example = "#FF5733")
    val colorStart: String?,

    @param:Schema(description = "배경 끝 색상", example = "#33C1FF")
    val colorEnd: String?,

    @param:Schema(description = "총 투표 수", example = "120")
    val voteCount: Int = 0,

    @param:Schema(description = "댓글 수", example = "15")
    val commentCount: Int = 0,

    @param:Schema(description = "좋아요 수", example = "20")
    val likeCount: Int = 0,

    @param:Schema(description = "내가 좋아요했는지 여부", example = "true")
    val likedByMe: Boolean = false,

    @param:Schema(description = "내가 투표했는지 여부", example = "true")
    val votedByMe: Boolean = false,

    @param:Schema(description = "내가 투표한 선택지 번호", example = "2")
    val votedSeq: Int? = null,

    @param:Schema(description = "투표 허용여부", example = "")
    val allowComment: Int,

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm")
    val createdAt: OffsetDateTime,

    val user: UserResponse?,
)

@Schema(description = "투표 댓글 등록/수정 요청 DTO")
data class PollCommentRegisterRequest(

    @param:Schema(description = "투표 ID", example = "101")
    val pollId: Long,

    @param:Schema(description = "댓글 UID (수정 시 필요)", example = "550e8400-e29b-41d4-a716-446655440000")
    val commentUid: UUID? = null,

    @field:NotBlank
    @param:Schema(description = "댓글 내용", example = "저는 이 영화가 최고라고 생각합니다!")
    val content: String,

    @param:Schema(description = "부모 댓글 ID (대댓글일 경우 지정)", example = "202")
    val parentId: Long? = null
)


@Schema(description = "투표 댓글 응답 정보")
data class PollCommentResponse(

    @JsonIgnore
    @param:Schema(description = "댓글 내부 ID (응답에 노출되지 않음)", example = "100")
    val id: Long,

    @param:Schema(description = "댓글 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val uid: UUID,

    @param:Schema(description = "댓글 내용", example = "저는 치킨에 한 표요!")
    val content: String,

    @param:Schema(description = "대댓글 개수", example = "3")
    val replyCount: Int = 0,

    @param:Schema(description = "좋아요 수", example = "12")
    val likeCount: Long = 0,

    @param:Schema(description = "내가 좋아요를 눌렀는지 여부", example = "true")
    val likedByMe: Boolean = false,

    @param:Schema(description = "부모 댓글 UID (대댓글일 경우만 존재)", example = "660e8400-e29b-41d4-a716-556655440111")
    val parentUid: UUID? = null,

    @JsonIgnore
    @param:Schema(description = "작성자 내부 ID (응답에 노출되지 않음)", example = "42")
    val createdId: Long,

    @param:Schema(description = "댓글 작성 시각", example = "2025-09-18T10:30:00Z")
    val createdAt: OffsetDateTime,

    @param:Schema(description = "작성자 요약 정보")
    val user: UserResponse? = null,
)
