package kr.jiasoft.hiteen.feature.poll.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.feature.poll.domain.PollEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import java.util.UUID

data class PollCreateRequest(
    val question: String,
    val answers: List<String>,      // ["치킨", "피자", "김밥"]
    val colorStart: String?,
    val colorEnd: String?,
    val allowComment: Int = 1,      // 0=허용안함, 1=허용
//    val onlyComment: Int = 0        // 1=댓글로 투표, 0=일반투표
)

data class PollUpdateRequest(
    val question: String?,
    val answers: List<String>?, // 선택지 수정
    val colorStart: String?,
    val colorEnd: String?,
    val allowComment: Boolean?,
)



data class PollVoteRequest(
    val seq: Int?
)


data class PollSelectResponse(
    val seq: Int,
    val answer: String,
    val votes: Int
)


data class PollSummaryRow(
    val id: Long?,
    val question: String?,
    val photo: String?,
    val selects: String?, // jsonb를 String으로 받음
    val colorStart: String?,
    val colorEnd: String?,
    val voteCount: Int?,
    val commentCount: Int?,
    val votedByMe: Boolean?,
    val userId: Long?,
//    val username: String?, // UserResponse정보 함께 조회?
//    val nickname: String?,
//    val profileImage: String?
)


data class PollResponse(
    val id: Long?,
    val question: String?,
    val photo: String?,
    val selects: List<PollSelectResponse>?,
    val colorStart: String?,
    val colorEnd: String?,
    val voteCount: Int?,
//    val likeCount: Int?,// TODO
    val commentCount: Int?,
    val votedByMe: Boolean?,
//    val likedByMe: Boolean?,// TODO
    val user: UserResponse?,
//    val comments: ApiPageCursor<PollCommentResponse>?,// TODO
) {
    companion object {
        fun of(entity: PollEntity, votedByMe: Boolean, user: UserResponse): PollResponse {
            val selects: List<PollSelectResponse> =
                try {
                    val raw = entity.selects?.asString() ?: "[]"
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
                votedByMe = votedByMe,
                user = user
            )
        }
    }
}


data class PollCommentRegisterRequest(
    val pollId: Long? = null,
    val commentUid: UUID? = null,
//    @field:NotBlank
    val content: String?,
    val parentId: Long? = null
)


data class PollCommentResponse(
    @JsonIgnore
    val id: Long? = null,
    val uid: UUID? = null,
    val content: String?,
    val replyCount: Int,
    val likedByMe: Boolean
)
