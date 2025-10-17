package kr.jiasoft.hiteen.feature.poll.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.r2dbc.postgresql.codec.Json
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.poll.domain.*
import kr.jiasoft.hiteen.feature.poll.dto.*
import kr.jiasoft.hiteen.feature.poll.infra.*
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.buildPushData
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.*

@Service
class PollService(
    private val polls: PollRepository,
    private val pollUsers: PollUserRepository,
    private val comments: PollCommentRepository,
    private val commentLikes: PollCommentLikeRepository,
    private val assetService: AssetService,
    private val objectMapper: ObjectMapper,
    private val pollLikes: PollLikeRepository,
    private val userService: UserService,

    private val expService: ExpService,
    private val pointService: PointService,
    private val pushService: PushService,
) {

    private enum class PollStatus {
        ACTIVE,
        INACTIVE
    }


    open suspend fun listPollsByCursor(
        cursor: Long?,
        size: Int,
        currentUserId: Long?
    ): List<PollResponse> =
        polls.findSummariesByCursor(cursor, size, currentUserId)
            .map { row ->
                val user = userService.findUserSummary(row.createdId)
                val voteCounts = pollUsers.countVotesByPollId(row.id).toList()
                    .associateBy({ it.seq }, { it.votes.toInt() })

                PollResponse.build(
                    id = row.id,
                    question = row.question,
                    photo = row.photo,
                    selectsJson = row.selects,
                    colorStart = row.colorStart,
                    colorEnd = row.colorEnd,
                    commentCount = row.commentCount,
                    createdAt = row.createdAt,
                    user = user,
                    votedSeq = row.votedSeq,
                    likeCount = row.likeCount,
                    likedByMe = row.likedByMe,
                    votedByMe = row.votedByMe,
                    voteCounts = voteCounts,
                    objectMapper = objectMapper
                )
            }.toList()


    suspend fun getPoll(id: Long, currentUserId: Long): PollResponse {
        val poll = polls.findSummaryById(id, currentUserId) ?: throw notFound("poll")

        val user = userService.findUserSummary(poll.createdId)

        val voteCounts = pollUsers.countVotesByPollId(id).toList()
            .associateBy({ it.seq }, { it.votes.toInt() })

        return PollResponse.build(
            id = poll.id,
            question = poll.question,
            photo = poll.photo,
            selectsJson = poll.selects,
            colorStart = poll.colorStart,
            colorEnd = poll.colorEnd,
            commentCount = poll.commentCount,
            createdAt = poll.createdAt,
            user = user,
            votedSeq = poll.votedSeq,
            likeCount = poll.likeCount,
            likedByMe = poll.likedByMe,
            voteCounts = voteCounts,
            objectMapper = objectMapper
        )
    }





    suspend fun create(req: PollCreateRequest, userId: Long, file: FilePart?): Long {
        val uploadedPhoto: String? = if (file != null) {
            val asset = assetService.uploadImage(
                file = file,
                originFileName = null,
                currentUserId = userId
            )
            asset.uid.toString()
        } else null

        val selects = req.answers.mapIndexed { idx, ans ->
            mapOf("seq" to (idx + 1), "answer" to ans, "votes" to 0)
        }

        // json 문자열 → R2DBC Json
        val selectsJson: Json = Json.of(objectMapper.writeValueAsString(selects))

        val saved = polls.save(
            PollEntity(
                question = req.question,
                photo = uploadedPhoto,
                selects = selectsJson,
                colorStart = req.colorStart,
                colorEnd = req.colorEnd,
                allowComment = req.allowComment,
                status = PollStatus.ACTIVE.name,
                createdId = userId,
                createdAt = OffsetDateTime.now()
            )
        )

        expService.grantExp(userId, "CREATE_VOTE", saved.id)
        pointService.applyPolicy(userId, PointPolicy.VOTE_QUESTION, saved.id)
        return saved.id
    }


    suspend fun update(id: Long?, req: PollUpdateRequest, userId: Long, file: FilePart?): Long {
        val poll = polls.findById(id!!) ?: throw notFound("poll")

        if (poll.createdId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only owner can update poll")
        }

        // 사진 교체
        val uploadedPhoto: String? = if (file != null) {
            val asset = assetService.uploadImage(file, userId, AssetCategory.POLL)
            asset.uid.toString()
        } else poll.photo

        // answers 수정 시 JSON 직렬화
        val selectsJson: Json? = req.answers?.let { answers ->
            val selects = answers.mapIndexed { idx, ans ->
                mapOf("seq" to (idx + 1), "answer" to ans, "votes" to 0)
            }
            Json.of(objectMapper.writeValueAsString(selects))
        }

        val updated = poll.copy(
            question = req.question ?: poll.question,
            photo = uploadedPhoto,
            selects = selectsJson ?: poll.selects,
            colorStart = req.colorStart ?: poll.colorStart,
            colorEnd = req.colorEnd ?: poll.colorEnd,
            allowComment = req.allowComment?.let { if (it) 1 else 0 } ?: poll.allowComment,
            updatedAt = OffsetDateTime.now(),
        )

        polls.save(updated)
        return updated.id
    }


    suspend fun softDelete(id: Long, currentUserId: Long) {
        val p = polls.findById(id) ?: throw notFound("board")
        if (p.createdId != currentUserId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only owner can delete poll")
        }
        val updated = p.copy(deletedAt = OffsetDateTime.now())
        polls.save(updated)
    }


    suspend fun vote(pollId: Long?, seq: Int, userId: Long) {
        val poll = polls.findById(pollId!!)
            ?: throw IllegalArgumentException( "poll not found")

        // ✅ ① 선택지 검증 (poll.selects JSON 내부에서 seq 존재 여부 확인)
        val mapper = objectMapper
        val selects: List<Map<String, Any>> = mapper.readValue(poll.selects.asString())

        val valid = selects.any { it["seq"]?.toString()?.toIntOrNull() == seq }
        if (!valid) {
            throw IllegalArgumentException("invalid choice")
        }

        // ✅ ② 중복 투표 예외 처리
        try {
            pollUsers.save(
                PollUserEntity(
                    pollId = pollId,
                    userId = userId,
                    seq = seq,
                    votedAt = OffsetDateTime.now()
                )
            )

            polls.increaseVoteCount(pollId)
            expService.grantExp(userId, "VOTE_PARTICIPATE", pollId)
        } catch (_: DuplicateKeyException) {
            throw BusinessValidationException(mapOf("error" to "already voted"))
        }
    }


    // ---------------------- Likes ----------------------
    suspend fun like(id: Long, currentUserId: Long) {
        val p = polls.findById(id) ?: throw notFound("board")
        try {
            pollLikes.save(PollLikeEntity(pollId = p.id, userId = currentUserId, createdAt = OffsetDateTime.now()))
            expService.grantExp(currentUserId, "LIKE_VOTE", p.id)
        } catch (_: DuplicateKeyException) {
        }
    }


    suspend fun unlike(id: Long, currentUserId: Long) {
        val b = polls.findById(id) ?: throw notFound("board")
        try {
            pollLikes.deleteByPollIdAndUserId(b.id, currentUserId)
        } catch (_: DuplicateKeyException) {
        }
    }


    // ---------------------- Comments ----------------------
    suspend fun listComments(
        pollId: Long,
        parentUid: UUID?,
        currentUserId: Long?,
        cursor: UUID?,
        perPage: Int
    ): List<PollCommentResponse>
            = comments.findComments(pollId, parentUid, currentUserId ?: -1L, cursor, perPage + 1)
                .map { comment ->
                        comment.copy(
                            user = userService.findUserSummary(comment.createdId)
                        )
                    }.toList()


    suspend fun createComment(req: PollCommentRegisterRequest, user: UserEntity): Long {
        val p = polls.findById(req.pollId)!!
        val parent: PollCommentEntity? = req.parentId?.let { comments.findById(it) }
        val saved = comments.save(
            PollCommentEntity(
                pollId = p.id,
                parentId = req.parentId,
                content = req.content,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )
        polls.increaseCommentCount(req.pollId)
        if (parent != null) comments.increaseReplyCount(parent.id)

        expService.grantExp(user.id, "CREATE_VOTE_COMMENT", saved.id)
        pointService.applyPolicy(user.id, PointPolicy.VOTE_COMMENT, saved.id)
        pushService.sendAndSavePush(listOf(p.createdId), PushTemplate.VOTE_COMMENT.buildPushData("nickname" to user.nickname))
        return saved.id
    }


    suspend fun updateComment(
        pollId: Long,
        commentUid: UUID,
        req: PollCommentRegisterRequest,
        currentUserId: Long
    ): UUID {
        val poll = polls.findById(pollId)
        val comment = comments.findByUid(commentUid) ?: throw notFound("comment")

        // 보드 소속 검증 + 삭제된 댓글 제외(DDL/엔티티에 deletedAt 있음을 가정)
        if (comment.pollId != poll?.id) throw badRequest("comment does not belong to this board")
        if (comment.deletedAt != null) throw badRequest("comment already deleted")

        // 권한: 작성자만 수정 (관리자 허용하고 싶으면 role 체크 추가)
        if (comment.createdId != currentUserId) throw forbidden("you are not the author")

        val trimmed = req.content.trim()
        trimmed.let { if (it.isEmpty()) throw badRequest("content must not be blank") }

        val merged = comment.copy(
            content = trimmed,
            updatedAt = OffsetDateTime.now()
        )
        comments.save(merged)
        return merged.uid
    }


    suspend fun deleteComment(
        pollId: Long,
        commentUid: UUID,
        currentUserId: Long
    ): UUID {
        val board = polls.findById(pollId)
        val comment = comments.findByUid(commentUid) ?: throw notFound("comment")

        // 보드 소속 검증 + 중복 삭제 방지
        if (comment.pollId != board?.id) throw badRequest("comment does not belong to this board")
        if (comment.deletedAt != null) throw badRequest("comment already deleted")
        if (comment.createdId != currentUserId) throw forbidden("you are not the author")

        val deleted = comment.copy(
            deletedAt = OffsetDateTime.now()
        )
        comments.save(deleted)
        polls.decreaseCommentCount(pollId)
        comment.parentId?.let { pid ->
            comments.decreaseReplyCount(pid)
        }

        return deleted.uid
    }


    suspend fun likeComment(commentUid: UUID, currentUserId: Long) {
        val c = comments.findByUid(commentUid) ?: throw notFound("comment")
        try {
            commentLikes.save(PollCommentLikeEntity(commentId = c.id, userId = currentUserId, createdAt = OffsetDateTime.now()))
            expService.grantExp(currentUserId, "LIKE_VOTE_COMMENT", c.id)
        } catch (_: DuplicateKeyException) {
        }
    }


    suspend fun unlikeComment(commentUid: UUID, currentUserId: Long) {
        val c = comments.findByUid(commentUid) ?: throw notFound("comment")
        commentLikes.deleteByCommentIdAndUserId(c.id, currentUserId)
    }


    private fun notFound(what: String) = ResponseStatusException(HttpStatus.BAD_REQUEST, "$what not found")
    private fun badRequest(msg: String) = ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
    private fun forbidden(msg: String) = ResponseStatusException(HttpStatus.FORBIDDEN, msg)
}
