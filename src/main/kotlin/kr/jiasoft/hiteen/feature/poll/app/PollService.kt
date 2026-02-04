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
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponseIncludes
import org.springframework.context.ApplicationEventPublisher
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
    private val friendRepository: FriendRepository,

    private val userService: UserService,
    private val assetService: AssetService,
    private val expService: ExpService,
    private val pointService: PointService,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private enum class PollStatus {
        ACTIVE,
        INACTIVE
    }


    open suspend fun listPollsByCursor(
        cursor: Long?,
        size: Int,
        currentUserId: Long?,
        type: String = "all",
        author: UUID?,
        orderType: String? = null,
        userLat: Double? = null,
        userLng: Double? = null,
        maxDistance: Double? = null,
        sortByDistance: Boolean = false,
        lastDistance: Double? = null,
        lastId: Long? = null,
    ): List<PollResponse> {
        val order = PollOrderType.from(orderType)
        val rows = polls.findSummariesByCursor(
            cursor, size, currentUserId, type, author, order.name,
            userLat, userLng, maxDistance, sortByDistance, lastDistance, lastId
        ).toList()
        if (rows.isEmpty()) return emptyList()

        val pollIds = rows.map { it.id }.toTypedArray()
        val pollIdList = rows.map { it.id }

        // 1) 사진(PollPhotos) 일괄 조회 (N+1 방지)
        val photosMap = pollPhotos.findAllByPollIdIn(pollIds).toList()
            .groupBy { it.pollId }
            .mapValues { (_, list) ->
                list.sortedBy { it.seq }.map { it.assetUid.toString() }
            }

        // 2) 선택지(PollSelects) 일괄 조회 (N+1 방지)
        val selectsMap = pollSelects.findSelectSummariesByPollIdIn(pollIds).toList()
            .groupBy { it.pollId }
            .mapValues { (_, list) ->
                list.map { p ->
                    PollSelectResponse(
                        id = p.id,
                        seq = p.seq,
                        content = p.content,
                        voteCount = p.voteCount,
                        photos = p.photoUid?.let { listOf(it.toString()) } ?: emptyList()
                    )
                }
            }

        // 3) 좋아요 수 일괄 조회 (N+1 방지)
        val likeCountMap = pollLikes.countBulkByPollIdIn(pollIdList).toList()
            .associate { it.id to it.count }

        // 4) 내가 좋아요/투표 했는지 여부 일괄 조회 (로그인 시, N+1 방지)
        val likedByMeSet = if (currentUserId != null) {
            pollLikes.findAllIdsByUserIdAndPollIdIn(currentUserId, pollIdList).toList().toSet()
        } else emptySet()

        val votedByMeMap = if (currentUserId != null) {
            pollUsers.findAllByUserIdAndPollIdIn(currentUserId, pollIdList).toList()
                .associateBy { it.pollId }
        } else emptyMap()

        // 5) 작성자 정보 일괄 조회 (N+1 방지)
        val authorIds = rows.map { it.createdId }.distinct()
        val userMap = userService.findUserResponseByIds(
            targetIds = authorIds,
            currentUserId = currentUserId,
            includes = UserResponseIncludes(school = true, tier = true)
        ).associateBy { it.id }

        return rows.map { row ->
            val selects = selectsMap[row.id] ?: emptyList()
            val totalVotes = selects.sumOf { it.voteCount }
            val voted = votedByMeMap[row.id]

            PollResponse(
                id = row.id,
                question = row.question,
                photos = photosMap[row.id] ?: emptyList(),
                selects = selects,
                colorStart = row.colorStart,
                colorEnd = row.colorEnd,
                voteCount = totalVotes,
                commentCount = row.commentCount,
                address = row.address,
                detailAddress = row.detailAddress,
                lat = row.lat,
                lng = row.lng,
                distance = row.distance,
                likeCount = likeCountMap[row.id] ?: 0,
                likedByMe = likedByMeSet.contains(row.id),
                votedByMe = voted != null,
                votedSeq = voted?.seq,
                allowComment = row.allowComment,
                createdAt = row.createdAt,
                deletedAt = row.deletedAt,
                user = userMap[row.createdId]
            )
        }
    }



    suspend fun getPoll(id: Long, currentUserId: Long): PollResponse {
        // ① 기본 요약 데이터 조회
        val poll = polls.findSummaryById(id, currentUserId) ?: throw IllegalArgumentException("해당 투표를 찾을 수 없어 😢")
        //삭제 된 투표 예외 처리
        poll.deletedAt?.let {
            throw IllegalArgumentException("이미 삭제된 투표야 😢")
        }
        val user = userService.findUserResponse(poll.createdId, includes = UserResponseIncludes(school = true, tier = true))

        // ② 본문 이미지 (pollPhotos)
        val photos = pollPhotos.findAllByPollId(id)
            .toList()
            .sortedBy { it.seq }
            .map { it.assetUid.toString() }

        // ③ 선택지 조회
        val selectEntities = pollSelects.findAllByPollId(id).toList().sortedBy { it.seq }
        val selectIds = selectEntities.map { it.id }

        // ④ 선택지 이미지 일괄 조회 (N+1 방지)
        val selectPhotosMap = pollSelectPhotos.findAllBySelectIdIn(selectIds).toList()
            .groupBy { it.selectId }
            .mapValues { (_, list) ->
                list.sortedBy { it.seq }.mapNotNull { it.assetUid?.toString() }
            }

        val selects = selectEntities.map { sel ->
            PollSelectResponse(
                id = sel.id,
                seq = sel.seq,
                content = sel.content,
                voteCount = sel.voteCount,
                photos = selectPhotosMap[sel.id] ?: emptyList()
            )
        }

        // ⑤ 총 투표 수 계산
        val totalVotes = selects.sumOf { it.voteCount }

        // ⑥ PollResponse 반환
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
            likedByMe = rowLikedByMe(poll),
            votedByMe = rowVotedByMe(poll),
            votedSeq = rowVotedSeq(poll),
            allowComment = poll.allowComment,
            createdAt = poll.createdAt,
            user = user,
        )
    }

    // Helper functions to handle PollSummaryRow
    private fun rowLikedByMe(p: PollSummaryRow) = p.likedByMe
    private fun rowVotedByMe(p: PollSummaryRow) = p.votedByMe
    private fun rowVotedSeq(p: PollSummaryRow) = p.votedSeq




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
        val regexPoll = Regex("poll_([1-9]\\d*)")
        val regexSelect = Regex("select_([1-9]\\d*)_([1-9]\\d*)")

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


    suspend fun create(req: PollCreateRequest, user: UserEntity, fileFlux: Flux<FilePart>?): Long {
        val pollEntity = PollEntity(
            question = req.question,
            colorStart = req.colorStart,
            colorEnd = req.colorEnd,
            allowComment = req.allowComment,
            address = req.address,
            detailAddress = req.detailAddress,
            lat = req.lat,
            lng = req.lng,
            status = PollStatus.ACTIVE.name,
            createdId = user.id,
            createdAt = OffsetDateTime.now()
        )

        val id = saveOrUpdatePoll(pollEntity, req.answers, user.id, fileFlux)

        expService.grantExp(user.id, "CREATE_VOTE", id)
        pointService.applyPolicy(user.id, PointPolicy.VOTE_QUESTION, id)
        //포스팅 알림
        val friendIds = friendRepository.findAllFriendship(user.id).toList()
        eventPublisher.publishEvent(
            PushSendRequestedEvent(
                userIds = friendIds,
                actorUserId = user.id,
                templateData = PushTemplate.NEW_VOTE.buildPushData("nickname" to user.nickname),
                extraData = mapOf("pollId" to id.toString()),
            )
        )
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
            address = req.address ?: poll.address,
            detailAddress = req.detailAddress ?: poll.detailAddress,
            lat = req.lat ?: poll.lat,
            lng = req.lng ?: poll.lng,
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
            address = req.address ?: poll.address,
            detailAddress = req.detailAddress ?: poll.detailAddress,
            lat = req.lat ?: poll.lat,
            lng = req.lng ?: poll.lng,
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
        val poll = polls.findById(pollId!!) ?: throw IllegalArgumentException( "해당 투표를 찾을 수 없습니다." )
        // 삭제 된 투표 예외 처리
        poll?.deletedAt?.let {
            throw IllegalArgumentException("이미 삭제된 투표입니다.")
        }

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
    ): List<PollCommentResponse> {
        val rawComments = comments.findComments(pollId, parentUid, currentUserId ?: -1L, cursor, perPage + 1).toList()
        if (rawComments.isEmpty()) return emptyList()

        val authorIds = rawComments.map { it.createdId }.distinct()
        val userMap = userService.findUserResponseByIds(
            targetIds = authorIds,
            currentUserId = currentUserId,
            includes = UserResponseIncludes(school = true)
        ).associateBy { it.id }

        return rawComments.map { comment ->
            comment.copy(
                user = userMap[comment.createdId]
            )
        }
    }

    suspend fun getComment(
        pollId: Long,
        commentUid: UUID,
        currentUserId: Long,
    ): PollCommentResponse? {
        val comment = comments.findComment(pollId, commentUid, currentUserId)
        return comment?.copy(
            user = userService.findUserResponse(comment.createdId)
        )
    }


    suspend fun createComment(req: PollCommentRegisterRequest, user: UserEntity): PollCommentResponse? {
        val p = polls.findById(req.pollId) ?: throw IllegalArgumentException("해당 투표를 찾을 수 없어 😢")
        //삭제 된 투표 예외 처리
        p.deletedAt?.let {
            throw IllegalArgumentException("이미 삭제된 투표야 😢")
        }
        if (p.allowComment == 0) throw BusinessValidationException(mapOf("error" to "comment_not_allowed"))

        val parent: PollCommentEntity? = req.parentUid?.let { uid -> comments.findByUid(uid) }
        val saved = comments.save(
            PollCommentEntity(
                pollId = p.id,
                parentId = parent?.id,
                content = req.content,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )
        polls.increaseCommentCount(req.pollId)
        parent?.let { comments.increaseReplyCount(it.id) }

        expService.grantExp(user.id, "CREATE_VOTE_COMMENT", saved.id)
        pointService.applyPolicy(user.id, PointPolicy.VOTE_COMMENT, saved.id)

        val extraData = mutableMapOf("pollId" to p.id.toString())
        parent?.let { extraData["parentUid"] = it.uid.toString() }

        //본인 글 아닐때만 알림
        if( p.createdId != user.id && ( parent == null || parent.createdId != p.createdId ) ) {
            eventPublisher.publishEvent(
                PushSendRequestedEvent(
                    userIds = listOf(p.createdId),
                    actorUserId = user.id,
                    templateData = PushTemplate.VOTE_COMMENT.buildPushData("nickname" to user.nickname),
                    extraData = extraData,
                )
            )
        }

        parent?.let { pr ->
            if (pr.createdId != user.id) {
                eventPublisher.publishEvent(
                    PushSendRequestedEvent(
                        userIds = listOf(pr.createdId),
                        actorUserId = user.id,
                        templateData = PushTemplate.VOTE_REPLY.buildPushData("nickname" to user.nickname),
                        extraData = extraData,
                    )
                )
            }
        }

        return getComment(req.pollId, saved.uid, user.id)
    }


    suspend fun updateComment(
        pollId: Long,
        commentUid: UUID,
        req: PollCommentRegisterRequest,
        currentUserId: Long
    ): UUID {
        val poll = polls.findById(pollId)
        val comment = comments.findByUid(commentUid) ?: throw notFound("comment")

        if (comment.pollId != poll?.id) throw badRequest("comment does not belong to this board")
        if (comment.deletedAt != null) throw badRequest("comment already deleted")
        if (comment.createdId != currentUserId) throw forbidden("you are not the author")

        val trimmed = req.content.trim()
        if (trimmed.isEmpty()) throw badRequest("content must not be blank")

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


    suspend fun listMyComments(
        userId: Long,
        cursor: UUID?,
        perPage: Int
    ): List<PollCommentResponse> {
        val rawComments = comments.findMyComments(userId, cursor, perPage).toList()
        if (rawComments.isEmpty()) return emptyList()

        val authorIds = rawComments.map { it.createdId }.distinct()
        val userMap = userService.findUserResponseByIds(
            targetIds = authorIds,
            currentUserId = userId,
            includes = UserResponseIncludes(school = true)
        ).associateBy { it.id }

        return rawComments.map { comment ->
            comment.copy(
                user = userMap[comment.createdId]
            )
        }
    }


    private fun notFound(what: String) = ResponseStatusException(HttpStatus.BAD_REQUEST, "$what not found")
    private fun badRequest(msg: String) = ResponseStatusException(HttpStatus.BAD_REQUEST, msg)
    private fun forbidden(msg: String) = ResponseStatusException(HttpStatus.FORBIDDEN, msg)
}
