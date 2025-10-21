package kr.jiasoft.hiteen.feature.poll.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
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
import reactor.core.publisher.Flux
import java.time.OffsetDateTime
import java.util.*

@Service
class PollService(
    private val polls: PollRepository,
    private val pollUsers: PollUserRepository,
    private val pollPhotos: PollPhotoRepository,
    private val comments: PollCommentRepository,
    private val commentLikes: PollCommentLikeRepository,
    private val pollLikes: PollLikeRepository,
    private val pollSelects: PollSelectRepository,
    private val pollSelectPhotos: PollSelectPhotoRepository,

    private val userService: UserService,
    private val assetService: AssetService,
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

                val photos = pollPhotos.findAllByPollId(row.id)
                    .toList()
                    .sortedBy { it.seq }
                    .map { it.assetUid.toString() }

                // ③ 선택지 이미지 조회 (pollSelectPhotos)
                val selects = pollSelects.findAllByPollId(row.id)
                    .toList()
                    .sortedBy { it.seq }
                    .map { select ->
                        val selectPhotos = pollSelectPhotos.findAllBySelectId(select.id)
                            .toList()
                            .sortedBy { it.seq }
                            .mapNotNull { it.assetUid?.toString() }

                        PollSelectResponse(
                            id = select.id,
                            seq = select.seq,
                            content = select.content,
                            voteCount = select.voteCount,
                            photos = selectPhotos
                        )
                    }

                // ④ 전체 투표수 계산
                val totalVotes = selects.sumOf { it.voteCount }

                // ⑤ 최종 응답 생성
                PollResponse(
                    id = row.id,
                    question = row.question,
                    photos = photos,
                    selects = selects,
                    colorStart = row.colorStart,
                    colorEnd = row.colorEnd,
                    voteCount = totalVotes,
                    commentCount = row.commentCount,
                    likeCount = row.likeCount,
                    likedByMe = row.likedByMe,
                    votedByMe = row.votedByMe,
                    votedSeq = row.votedSeq,
                    allowComment = row.allowComment,
                    createdAt = row.createdAt,
                    user = user,
                )

            }.toList()


    suspend fun getPoll(id: Long, currentUserId: Long): PollResponse {
        // ① 기본 요약 데이터 조회
        val poll = polls.findSummaryById(id, currentUserId) ?: throw notFound("poll")
        val user = userService.findUserSummary(poll.createdId)

        // ② 본문 이미지 (pollPhotos)
        val photos = pollPhotos.findAllByPollId(id)
            .toList()
            .sortedBy { it.seq }
            .map { it.assetUid.toString() }

        // ③ 선택지 + 선택지 이미지
        val selects = pollSelects.findAllByPollId(id)
            .toList()
            .sortedBy { it.seq }
            .map { sel ->
                val selectPhotos = pollSelectPhotos.findAllBySelectId(sel.id)
                    .toList()
                    .sortedBy { it.seq }
                    .mapNotNull { it.assetUid?.toString() }

                PollSelectResponse(
                    id = sel.id,
                    seq = sel.seq,
                    content = sel.content,
                    voteCount = sel.voteCount,
                    photos = selectPhotos
                )
            }

        // ④ 총 투표 수 계산
        val totalVotes = selects.sumOf { it.voteCount }

        // ⑤ PollResponse 반환
        return PollResponse(
            id = poll.id,
            question = poll.question,
            photos = photos,
            selects = selects,
            colorStart = poll.colorStart,
            colorEnd = poll.colorEnd,
            voteCount = totalVotes,
            commentCount = poll.commentCount,
            likeCount = poll.likeCount,
            likedByMe = poll.likedByMe,
            votedByMe = poll.votedByMe,
            votedSeq = poll.votedSeq,
            allowComment = poll.allowComment,
            createdAt = poll.createdAt,
            user = user,
        )
    }




    // ---------------- 공통 로직 ----------------

    private suspend fun savePollSelects(pollId: Long, answers: List<String>): List<PollSelectEntity> {
        return answers.mapIndexed { idx, ans ->
            pollSelects.save(
                PollSelectEntity(
                    pollId = pollId,
                    seq = (idx + 1),
                    content = ans
                )
            )
        }
    }

    private suspend fun savePollImages(
        pollId: Long,
        selectEntities: List<PollSelectEntity>,
        userId: Long,
        fileFlux: Flux<FilePart>?
    ) {
        val regexSelect = Regex("select_([1-9]\\d*)_([1-9]\\d*)")
        val regexPoll = Regex("poll_([1-9]\\d*)")

        val files = fileFlux?.collectList()?.awaitSingle().orEmpty()
        for (file in files) {
            val filename = file.filename()

            when {
                // ✅ 본문 이미지
                regexPoll.containsMatchIn(filename) -> {
                    val match = regexPoll.find(filename) ?: continue
                    val pollNumber = match.groupValues[1].toIntOrNull() ?: continue
                    if (pollNumber <= 0) continue

                    val uploaded = assetService.uploadImage(file, userId, AssetCategory.POLL_MAIN)
                    pollPhotos.save(
                        PollPhotoEntity(
                            pollId = pollId,
                            assetUid = uploaded.uid,
                            seq = pollNumber.toShort()
                        )
                    )
                }

                // ✅ 선택지 이미지
                regexSelect.containsMatchIn(filename) -> {
                    val match = regexSelect.find(filename) ?: continue
                    val seq = match.groupValues[1].toIntOrNull() ?: continue
                    val pollNum = match.groupValues[2].toIntOrNull() ?: continue
                    if (seq <= 0 || pollNum <= 0) continue

                    val select = selectEntities.find { it.seq == seq } ?: continue

                    val uploaded = assetService.uploadImage(file, userId, AssetCategory.POLL_SELECT)
                    pollSelectPhotos.save(
                        PollSelectPhotoEntity(
                            selectId = select.id,
                            assetUid = uploaded.uid,
                            seq = select.seq.toShort()
                        )
                    )
                }
            }
        }
    }


    private suspend fun saveOrUpdatePoll(
        pollEntity: PollEntity,
        answers: List<String>,
        userId: Long,
        fileFlux: Flux<FilePart>?
    ): Long {
        val saved = polls.save(pollEntity)
        val selects = savePollSelects(saved.id, answers)
        savePollImages(saved.id, selects, userId, fileFlux)
        return saved.id
    }


    suspend fun create(req: PollCreateRequest, userId: Long, fileFlux: Flux<FilePart>?): Long {
        val pollEntity = PollEntity(
            question = req.question,
            colorStart = req.colorStart,
            colorEnd = req.colorEnd,
            allowComment = req.allowComment,
            status = PollStatus.ACTIVE.name,
            createdId = userId,
            createdAt = OffsetDateTime.now()
        )

        val id = saveOrUpdatePoll(pollEntity, req.answers, userId, fileFlux)

        expService.grantExp(userId, "CREATE_VOTE", id)
        pointService.applyPolicy(userId, PointPolicy.VOTE_QUESTION, id)

        return id
    }




    /**
     * 투표 메타데이터(제목, 색상, 댓글 허용 여부 등)만 간단히 수정하는 함수
     * - 이미 투표가 진행 중이어도 수정 가능
     * - 이미지, 선택지, 파일 변경 없음
     */
    suspend fun updateMeta(id: Long?, req: PollUpdateRequest, userId: Long): Long {
        val poll = polls.findById(id!!) ?: throw notFound("poll")

        // ✅ 작성자 검증
        if (poll.createdId != userId) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only owner can update poll")
        }

        val votedCount = pollUsers.countByPollId(poll.id)
        if (votedCount > 0) throw IllegalStateException("already_voted, you can not update")


        // ✅ 간단한 필드만 수정
        val updatedPoll = poll.copy(
            question = req.question ?: poll.question,
            colorStart = req.colorStart ?: poll.colorStart,
            colorEnd = req.colorEnd ?: poll.colorEnd,
            allowComment = req.allowComment?.let { if (it) 1 else 0 } ?: poll.allowComment,
            updatedAt = OffsetDateTime.now()
        )

        polls.save(updatedPoll)
        return updatedPoll.id
    }



    /**
     * 투표 수정
     * -
     * */
    suspend fun update(id: Long?, req: PollUpdateRequest, userId: Long, fileFlux: Flux<FilePart>?): Long {
        val poll = polls.findById(id!!) ?: throw notFound("poll")

        if (poll.createdId != userId)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only owner can update poll")


        val votedCount = pollUsers.countByPollId(poll.id)
        if (votedCount > 0) throw IllegalStateException("already_voted, you can not update")


        // 기존 데이터 정리
        pollSelects.deleteAllByPollId(poll.id)
        pollSelectPhotos.deleteAllByPollId(poll.id)
        pollPhotos.deleteAllByPollId(poll.id)

        val updatedPoll = poll.copy(
            question = req.question ?: poll.question,
            colorStart = req.colorStart ?: poll.colorStart,
            colorEnd = req.colorEnd ?: poll.colorEnd,
            allowComment = req.allowComment?.let { if (it) 1 else 0 } ?: poll.allowComment,
            updatedAt = OffsetDateTime.now()
        )

        return saveOrUpdatePoll(updatedPoll, req.answers ?: emptyList(), userId, fileFlux)
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
        polls.findById(pollId!!) ?: throw IllegalArgumentException( "poll not found")

        val select = pollSelects.findAllByPollId(pollId)
            .toList()
            .find { it.seq == seq }
            ?: throw IllegalArgumentException("invalid choice")

        // ② 중복 투표 예외 처리
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
            pollSelects.increaseVoteCount(select.id)
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
        if(p.allowComment == 0) throw BusinessValidationException(mapOf("error" to "comment_not_allowed"))
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
