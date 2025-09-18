package kr.jiasoft.hiteen.feature.poll.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.feature.poll.domain.PollEntity
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import java.time.OffsetDateTime
import java.util.UUID


data class PollCreateRequest(
    @param:Schema(description = "질문") val question: String,
    @param:Schema(description = "답변 목록") val answers: List<String>,
    @param:Schema(description = "시작 색상") val colorStart: String? = null,
    @param:Schema(description = "끝 색상") val colorEnd: String? = null,
    @param:Schema(description = "댓글 허용 여부 (0=불가,1=가능)") val allowComment: Int
    //    val onlyComment: Int = 0        // 1=댓글로 투표, 0=일반투표
)


data class PollUpdateRequest(
    val id: Long,
    val question: String?,
    val answers: List<String>?, // 선택지 수정
    val colorStart: String?,
    val colorEnd: String?,
    val allowComment: Boolean?,
)



data class PollVoteRequest(
    val pollId: Long,
    val seq: Int
)


data class PollSelectResponse(
    val seq: Int,
    val answer: String,
    val votes: Int
)


data class PollSummaryRow(
    val id: Long,
    val question: String,
    val photo: String?,
    val selects: String?, // jsonb를 String으로 받음
    val colorStart: String?,
    val colorEnd: String?,
    val voteCount: Int = 0,
    val commentCount: Int = 0,
    val votedByMe: Boolean = false,
    val votedSeq: Int? = null,
    val createdId: Long,
    val createdAt: OffsetDateTime,
)


data class PollResponse(
    val id: Long,
    val question: String,
    val photo: String?,
    val selects: List<PollSelectResponse>?,
    val colorStart: String?,
    val colorEnd: String?,
    val voteCount: Int = 0,
//    val likeCount: Int?,// TODO
    val commentCount: Int = 0,
    val votedByMe: Boolean = false,
    val votedSeq: Int? = null,
    val createdAt: OffsetDateTime,
//    val likedByMe: Boolean?,// TODO
    val user: UserSummary?,
//    val comments: ApiPageCursor<PollCommentResponse>?,// TODO
) {
    companion object {
        fun of(entity: PollEntity, votedSeq: Int?, user: UserSummary): PollResponse {
            val selects: List<PollSelectResponse> =
                try {
                    val raw = entity.selects.asString()
                    jacksonObjectMapper().readValue(raw, object : TypeReference<List<PollSelectResponse>>() {})
                } catch (_: Exception) {
                    emptyList()
                }
            return PollResponse(
                id = entity.id,
                question = entity.question,
                photo = entity.photo,
                selects = selects,
                colorStart = entity.colorStart,
                colorEnd = entity.colorEnd,
                voteCount = entity.voteCount,
                commentCount = entity.commentCount,
                votedByMe = votedSeq != null,
                votedSeq = votedSeq,
                createdAt = entity.createdAt,
                user = user
            )
        }

        fun from(res: PollSummaryRow, user: UserSummary): PollResponse {
            val selects: List<PollSelectResponse> =
                try {
                    jacksonObjectMapper().readValue(res.selects, object : TypeReference<List<PollSelectResponse>>() {})
                } catch (_: Exception) {
                    emptyList()
                }
            return PollResponse(
                id = res.id,
                question = res.question,
                photo = res.photo,
                selects = selects,
                colorStart = res.colorStart,
                colorEnd = res.colorEnd,
                voteCount = res.voteCount,
                commentCount = res.commentCount,
                votedByMe = res.votedByMe,
                votedSeq = res.votedSeq,
                createdAt = res.createdAt,
                user = user
            )
        }
    }
}


data class PollCommentRegisterRequest(
    val pollId: Long,
    val commentUid: UUID? = null,
    @field:NotBlank
    val content: String,
    val parentId: Long? = null
)


data class PollCommentResponse(
    @JsonIgnore
    val id: Long,
    val uid: UUID,
    val content: String,
    val replyCount: Int = 0,
    val likeCount: Long = 0,
    val likedByMe: Boolean = false,
    val parentUid: UUID? = null,
    @JsonIgnore
    val createdId: Long,
    val createdAt: OffsetDateTime,
    val user: UserSummary? = null,
)
