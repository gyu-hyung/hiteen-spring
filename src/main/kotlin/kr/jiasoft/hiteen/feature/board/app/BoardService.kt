package kr.jiasoft.hiteen.feature.board.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.asset.app.event.AssetThumbnailPrecreateRequestedEvent
import kr.jiasoft.hiteen.feature.asset.domain.ThumbnailMode
import kr.jiasoft.hiteen.feature.board.domain.BoardAssetEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardBannerType
import kr.jiasoft.hiteen.feature.board.domain.BoardCategory
import kr.jiasoft.hiteen.feature.board.domain.BoardCommentEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardCommentLikeEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardLikeEntity
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentRegisterRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardCreateRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardUpdateRequest
import kr.jiasoft.hiteen.feature.board.infra.BoardAssetRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardBannerReadRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardCommentLikeRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardCommentRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardLikeRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BoardService(
    private val boards: BoardRepository,
    private val boardAssetRepository: BoardAssetRepository,
    private val boardBannerReadRepository: BoardBannerReadRepository,
    private val comments: BoardCommentRepository,
    private val likes: BoardLikeRepository,
    private val commentLikes: BoardCommentLikeRepository,
    private val assetService: AssetService,
    private val userService: UserService,
    private val expService: ExpService,
    private val pointService: PointService,
    private val eventPublisher: ApplicationEventPublisher,

    private val followRepository: FollowRepository,
    private val txOperator: TransactionalOperator,
) {

    private fun requestPostThumb800(uid: UUID, currentUserId: Long) {
        eventPublisher.publishEvent(
            AssetThumbnailPrecreateRequestedEvent(
                assetUids = listOf(uid),
                width = 800,
                height = 800,
                mode = kr.jiasoft.hiteen.feature.asset.domain.ThumbnailMode.COVER,
                requestedByUserId = currentUserId,
            )
        )
    }

    suspend fun getBoard(uid: UUID, currentUserId: Long?): BoardResponse {

        val userId = currentUserId ?: -1L
        val b = boards.findDetailByUid(uid, userId) ?: throw IllegalArgumentException("해당 글을 찾을 수 없어 😢")
        b.deletedAt?.let {
            throw IllegalArgumentException("이미 삭제된 글이야 😢")
        }
        val userSummary = userService.findUserSummary(b.createdId)

        val perPage = 15
        val rawComments = comments.findComments(b.uid, null, userId, null, perPage + 1).toList()

        // 댓글 작성자 정보 일괄 조회
        val commentAuthorIds = rawComments.map { it.createdId }.distinct()
        val commentAuthorMap = userService.findUserSummaryByIds(commentAuthorIds).associateBy { it.id }

        val commentList = rawComments.map { comment ->
            comment.copy(
                user = commentAuthorMap[comment.createdId]
            )
        }

        val hasMore = commentList.size > perPage
        val items = if (hasMore) commentList.dropLast(1) else commentList
        val nextCursor = if (hasMore) commentList.lastOrNull()?.uid?.toString() else null

        // 조회수 증가
        b.id.let { boards.increaseHits(it) }


        // 공지사항/이벤트 확인 시 경험치 부여
        if(b.category == "NOTICE" || b.category == "EVENT") {
            expService.grantExp(userId, "NOTICE_READ", b.id)
        }

        val withBanners = if (b.category == BoardCategory.EVENT.name || b.category == BoardCategory.EVENT_WINNING.name) {
            val bannerRows = boardBannerReadRepository.findAllByBoardIdIn(arrayOf(b.id)).toList()
            val large = bannerRows.filter { it.bannerType == BoardBannerType.LARGE.name }.map { it.uid }
            val small = bannerRows.filter { it.bannerType == BoardBannerType.SMALL.name }.map { it.uid }
            b.copy(largeBanners = large, smallBanners = small)
        } else {
            b
        }

        return withBanners.copy(
            content = b.content,
            hits = (b.hits) + 1,
            user = userSummary,
            comments = ApiPageCursor(
                nextCursor = nextCursor,
                items = items,
                perPage = perPage
            )
        )
    }


    suspend fun listBoardsByPage(
        category: String?, q: String?, page: Int, size: Int, currentUserId: Long?,
        followOnly: Boolean, friendOnly: Boolean, sameSchoolOnly: Boolean,
        status: String?,
        displayStatus: String?,
    ): ApiPage<BoardResponse> {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 100)
        val offset = p * s
        val uid = currentUserId ?: -1L

        // 총 개수
        val total = boards.countSearchResults(category, q, uid, followOnly, friendOnly, sameSchoolOnly, status, displayStatus)
        val lastPage = if (total == 0) 0 else (total - 1) / s

        val rows = boards.searchSummariesByPage(category, q, s, offset, uid, followOnly, friendOnly, sameSchoolOnly, status, displayStatus)
            .toList()

        // 유저 정보 일괄 조회 (N+1 방지)
        val authorIds = rows.map { it.createdId }.distinct()
        val userMap = userService.findUserSummaryByIds(authorIds).associateBy { it.id }

        val mapped = rows.map { row ->
            row.copy(
                content = row.content.take(160),
                user = userMap[row.createdId]
            )
        }

        // EVENT 배너 분리 매핑(배치 조회)
        val eventIds = mapped.filter { it.category == BoardCategory.EVENT.name || it.category == BoardCategory.EVENT_WINNING.name }
            .map { it.id }
        val bannerMap: Map<Long, Pair<List<UUID>, List<UUID>>> = if (eventIds.isEmpty()) {
            emptyMap()
        } else {
            val bannerRows = boardBannerReadRepository.findAllByBoardIdIn(eventIds.toTypedArray()).toList()
            bannerRows.groupBy { it.boardId }.mapValues { (_, list) ->
                val large = list.filter { it.bannerType == BoardBannerType.LARGE.name }.map { it.uid }
                val small = list.filter { it.bannerType == BoardBannerType.SMALL.name }.map { it.uid }
                large to small
            }
        }

        val finalRows = mapped.map { r ->
            if (r.category == BoardCategory.EVENT.name || r.category == BoardCategory.EVENT_WINNING.name) {
                val (large, small) = bannerMap[r.id] ?: (emptyList<UUID>() to emptyList())
                r.copy(largeBanners = large, smallBanners = small)
            } else r
        }

        return ApiPage(
            total = total,
            lastPage = lastPage,
            items = finalRows,
            perPage = s,
            currentPage = p
        )
    }


    suspend fun listBoardsByCursor(
        category: BoardCategory, q: String?, size: Int, userId: Long,
        followOnly: Boolean, friendOnly: Boolean, sameSchoolOnly: Boolean,
        cursorUid: UUID?, authorUid: UUID?
    ): ApiPageCursor<BoardResponse> {
        val s = size.coerceIn(1, 100)

        val rows = boards.searchSummariesByCursor(
            category.name, q, s + 1, userId, followOnly, friendOnly, sameSchoolOnly, cursorUid, authorUid
        ).toList()

        val hasMore = rows.size > s
        val items = if (hasMore) rows.take(s) else rows
        val nextCursor = if (hasMore) rows[s].uid.toString() else null

        // 특정 회원의 게시글 조회 시 경험치++(프로필 조회 화면)
        authorUid?.let {
            userService.findByUid(it.toString())?.let { user ->
                expService.grantExp(userId, "FRIEND_PROFILE_VISIT", user.id)
            }
        }

        // 유저 정보 일괄 조회 (N+1 방지)
        val authorIds = items.map { it.createdId }.distinct()
        val userMap = userService.findUserSummaryByIds(authorIds).associateBy { it.id }

        val mapped = items.map { row ->
            row.copy(
                content = row.content.take(160),
                user = userMap[row.createdId]
            )
        }

        val eventIds = mapped.filter { it.category == BoardCategory.EVENT.name || it.category == BoardCategory.EVENT_WINNING.name }
            .map { it.id }
        val bannerMap: Map<Long, Pair<List<UUID>, List<UUID>>> = if (eventIds.isEmpty()) {
            emptyMap()
        } else {
            val bannerRows = boardBannerReadRepository.findAllByBoardIdIn(eventIds.toTypedArray()).toList()
            bannerRows.groupBy { it.boardId }.mapValues { (_, list) ->
                val large = list.filter { it.bannerType == BoardBannerType.LARGE.name }.map { it.uid }
                val small = list.filter { it.bannerType == BoardBannerType.SMALL.name }.map { it.uid }
                large to small
            }
        }

        val finalItems = mapped.map { r ->
            if (r.category == BoardCategory.EVENT.name || r.category == BoardCategory.EVENT_WINNING.name) {
                val (large, small) = bannerMap[r.id] ?: (emptyList<UUID>() to emptyList())
                r.copy(largeBanners = large, smallBanners = small)
            } else r
        }

        return ApiPageCursor(
            nextCursor = nextCursor,
            items = finalItems,
            perPage = s
        )
    }



    suspend fun create(
        req: BoardCreateRequest,
        user: UserEntity,
        files: List<FilePart>,
        ip: String?
    ): UUID {
        // 1) ✅ 트랜잭션 밖에서 업로드 + (파일 1개 완료마다) 썸네일 이벤트 발행
        // - assets 저장은 개별 트랜잭션/커밋로 처리되므로, 리스너에서 UID 조회 시 '커밋 전 미노출' 레이스가 사라짐
        val uploaded: MutableList<AssetResponse> = mutableListOf()
        for (f in files) {
            val a = assetService.uploadImage(f, user.id, AssetCategory.POST)
            uploaded.add(a)
            requestPostThumb800(a.uid, user.id)
        }
        val representativeUid: UUID? = uploaded.firstOrNull()?.uid

        // 2) ✅ 게시글/매핑/경험치/포인트는 원자적으로 처리
        return txOperator.executeAndAwait {
            val saved = boards.save(
                BoardEntity(
                    category = req.category.name,
                    subject = req.subject,
                    content = req.content,
                    link = req.link,
                    ip = ip,
                    startDate = req.startDate,
                    endDate = req.endDate,
                    status = req.status,
                    address = req.address,
                    detailAddress = req.detailAddress,
                    lat = req.lat,
                    lng = req.lng,
                    assetUid = representativeUid,
                    createdId = user.id,
                    createdAt = OffsetDateTime.now(),
                )
            )

            if (uploaded.isNotEmpty()) {
                uploaded.forEach { a ->
                    boardAssetRepository.save(
                        BoardAssetEntity(
                            boardId = saved.id,
                            uid = a.uid
                        )
                    )
                }
            }

            expService.grantExp(user.id, "CREATE_BOARD", saved.id)
            pointService.applyPolicy(user.id, PointPolicy.STORY_POST, saved.id)

            val followerIds = followRepository.findAllFollowerIds(user.id).toList()
            if (followerIds.isNotEmpty()) {
                eventPublisher.publishEvent(
                    PushSendRequestedEvent(
                        userIds = followerIds,
                        actorUserId = user.id,
                        templateData = PushTemplate.NEW_POST.buildPushData(
                            "nickname" to user.nickname,
                        ),
                        extraData = mapOf("boardUid" to saved.uid.toString()),
                    )
                )
            }

            saved.uid
        }
    }

    /**
     * 파일까지 함께 처리하는 업데이트.
     * - deleteAssetUids: 부분 삭제할 자산 UID 목록
     * - replaceAssets:
     *    true  -> 모든 매핑 삭제 후 files만 등록
     *    false -> 기존 유지 + deleteAssetUids만 부분 삭제 + files 추가
     * 대표이미지 규칙:
     *  - files가 있으면 첫 번째 업로드 파일을 대표이미지로 설정
     *  - replaceAssets=true 이고 files가 없으면 대표이미지 제거(null)
     *  - replaceAssets=false 에서 부분삭제로 기존 대표가 지워졌고 files도 없으면
     *      남아있는 자산 중 하나(가장 최근/가장 오래된 등 정책)에 맞춰 대표를 재지정
     */
    suspend fun update(
        uid: UUID?,
        req: BoardUpdateRequest,
        currentUserId: Long,
        files: List<FilePart> = emptyList(),
        ip: String? = null,
        replaceAssets: Boolean? = false,
        deleteAssetUids: List<UUID>?
    ) {
        val b = boards.findByUid(uid!!) ?: throw notFound("board")
        val boardId = b.id

        // 1) 에셋 매핑 삭제
        val toDelete = deleteAssetUids?.distinct().orEmpty()
        if (replaceAssets == true) {
            boardAssetRepository.deleteByBoardId(boardId)
        } else if (toDelete.isNotEmpty()) {
            boardAssetRepository.deleteByBoardIdAndUidIn(boardId, toDelete)
        }

        // 2) 파일 업로드 -> 매핑 추가 + 대표이미지 후보(첫 번째 업로드)
        val uploadedUids: List<UUID> = if (files.isNotEmpty()) {
            val uploadedFiles: MutableList<AssetResponse> = mutableListOf()
            for (f in files) {
                val a = assetService.uploadImage(f, currentUserId, AssetCategory.POST)
                uploadedFiles.add(a)
                requestPostThumb800(a.uid, currentUserId)
            }
            val uids = uploadedFiles.map { it.uid }

            uploadedFiles.forEach { a ->
                boardAssetRepository.save(
                    BoardAssetEntity(
                        boardId = boardId,
                        uid = a.uid,
                    )
                )
            }
            uids
        } else {
            emptyList()
        }

        // 3) 대표 이미지(assetUid) 결정
        // - 새 파일이 있으면 첫 번째 업로드 파일을 대표로
        // - replaceAssets=true이고 새 파일이 없으면 대표 제거(null)
        // - 그 외에는 남아있는 첨부 중 최신(없으면 null)으로 대표 재지정
        val newAssetUid: UUID? = when {
            uploadedUids.isNotEmpty() -> uploadedUids.first()
            replaceAssets == true -> null
            else -> boardAssetRepository.findTopUidByBoardIdOrderByIdDesc(boardId)
        }

        // 4) 본문/메타 갱신 저장
        val merged = b.copy(
            category = req.category ?: b.category,
            subject = req.subject ?: b.subject,
            content = req.content ?: b.content,
            link = req.link ?: b.link,
            assetUid = newAssetUid,
            startDate = req.startDate ?: b.startDate,
            endDate = req.endDate ?: b.endDate,
            status = req.status ?: b.status,
            address = req.address ?: b.address,
            detailAddress = req.detailAddress ?: b.detailAddress,
            lat = req.lat,
            lng = req.lng,
            ip = ip ?: b.ip,
            updatedId = currentUserId,
            updatedAt = OffsetDateTime.now(),
        )
        boards.save(merged)
    }


    suspend fun softDelete(uid: UUID, currentUserId: Long) {
        val b = boards.findByUid(uid) ?: throw notFound("board")
        val updated = b.copy(deletedId = currentUserId, deletedAt = OffsetDateTime.now())
        boards.save(updated)
    }

    // ---------------------- Likes ----------------------
    suspend fun like(uid: UUID, currentUserId: Long) {
        val b = boards.findByUid(uid) ?: throw notFound("board")
        try {
            likes.save(BoardLikeEntity(boardId = b.id, userId = currentUserId, createdAt = OffsetDateTime.now()))
            expService.grantExp(currentUserId, "LIKE_BOARD", b.id)
        } catch (_: DuplicateKeyException) {
        }
    }

    //    @Transactional
    suspend fun unlike(uid: UUID, currentUserId: Long) {
        val b = boards.findByUid(uid) ?: throw notFound("board")
        try {
            likes.deleteByBoardIdAndUserId(b.id, currentUserId)
        } catch (_: DuplicateKeyException) {
        }
    }

    // ---------------------- Comments ----------------------
    suspend fun listComments(
        boardUid: UUID,
        parentUid: UUID?,
        currentUserId: Long?,
        cursor: UUID?,
        perPage: Int
    ): List<BoardCommentResponse> {
        val rawComments = comments.findComments(boardUid, parentUid, currentUserId ?: -1L, cursor, perPage).toList()
        if (rawComments.isEmpty()) return emptyList()

        val authorIds = rawComments.map { it.createdId }.distinct()
        val userMap = userService.findUserSummaryByIds(authorIds).associateBy { it.id }

        return rawComments.map { comment ->
            comment.copy(
                user = userMap[comment.createdId]
            )
        }
    }


    suspend fun getComment(
        boardUid: UUID,
        commentUid: UUID,
        currentUserId: Long,
    ): BoardCommentResponse? = comments.findComment(boardUid, commentUid, currentUserId).let { comment ->
        comment?.copy(
            user = userService.findUserSummary(comment.createdId)
        )
    }


    suspend fun listMyComments(
        userId: Long,
        cursor: UUID?,
        perPage: Int
    ): List<BoardCommentResponse> {
        val rawComments = comments.findMyComments(userId, cursor, perPage).toList()
        if (rawComments.isEmpty()) return emptyList()

        val authorIds = rawComments.map { it.createdId }.distinct()
        val userMap = userService.findUserSummaryByIds(authorIds).associateBy { it.id }

        return rawComments.map { comment ->
            comment.copy(
                user = userMap[comment.createdId]
            )
        }
    }


    suspend fun createComment(boardUid: UUID, req: BoardCommentRegisterRequest, user: UserEntity): BoardCommentResponse? {
        val b = boards.findByUid(boardUid) ?: throw notFound("board")
        val parent: BoardCommentEntity? = req.parentUid?.let { comments.findByUid(it) }
        val saved = comments.save(
            BoardCommentEntity(
                boardId = b.id,
                parentId = parent?.id,
                content = req.content,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )
        parent?.let { comments.increaseReplyCount(it.id) }

        expService.grantExp(user.id, "CREATE_BOARD_COMMENT", saved.id)
        pointService.applyPolicy(user.id, PointPolicy.STORY_COMMENT, saved.id)

        val extraData = mutableMapOf(
            "boardUid" to b.uid.toString()
        )

        parent?.let { extraData["parentUid"] = it.uid.toString() }

        //본인 글 아닐때만 알림
        if( b.createdId != user.id && ( parent == null || parent.createdId != b.createdId ) ) {
            eventPublisher.publishEvent(
                PushSendRequestedEvent(
                    userIds = listOf(b.createdId),
                    actorUserId = user.id,
                    templateData = PushTemplate.BOARD_COMMENT.buildPushData("nickname" to user.nickname),
                    extraData = extraData,
                )
            )
        }

        parent?.let {
            if( it.createdId == user.id ) return@let
            eventPublisher.publishEvent(
                PushSendRequestedEvent(
                    userIds = listOf(it.createdId),
                    actorUserId = user.id,
                    templateData = PushTemplate.BOARD_REPLY.buildPushData("nickname" to user.nickname),
                    extraData = extraData,
                )
            )
        }

        return getComment(b.uid, saved.uid, user.id)
    }


    suspend fun updateComment(
        boardUid: UUID,
        commentUid: UUID,
        req: BoardCommentRegisterRequest,
        currentUserId: Long
    ): UUID {
        val board = boards.findByUid(boardUid) ?: throw notFound("board")
        val comment = comments.findByUid(commentUid) ?: throw notFound("comment")

        // 보드 소속 검증 + 삭제된 댓글 제외(DDL/엔티티에 deletedAt 있음을 가정)
        if (comment.boardId != board.id) throw badRequest("comment does not belong to this board")
        if (comment.deletedAt != null) throw badRequest("comment already deleted")

        // 권한: 작성자만 수정 (관리자 허용하고 싶으면 role 체크 추가)
        if (comment.createdId != currentUserId) throw forbidden("you are not the author")

        val trimmed = req.content.trim()
        trimmed.let { if (it.isEmpty()) throw badRequest("content must not be blank") }

        val merged = comment.copy(
            content = trimmed,
            updatedId = currentUserId,
            updatedAt = OffsetDateTime.now()
        )
        comments.save(merged)
        return merged.uid
    }


    suspend fun deleteComment(
        boardUid: UUID,
        commentUid: UUID,
        currentUserId: Long
    ): UUID {
        val board = boards.findByUid(boardUid) ?: throw notFound("board")
        val comment = comments.findByUid(commentUid) ?: throw notFound("comment")

        // 보드 소속 검증 + 중복 삭제 방지
        if (comment.boardId != board.id) throw badRequest("comment does not belong to this board")
        if (comment.deletedAt != null) throw badRequest("comment already deleted")
        if (comment.createdId != currentUserId) throw forbidden("you are not the author")

        val deleted = comment.copy(
            deletedId = currentUserId,
            deletedAt = OffsetDateTime.now()
        )
        comments.save(deleted)
        comment.parentId?.let { pid ->
            comments.decreaseReplyCount(pid)
        }

        return deleted.uid
    }


    suspend fun likeComment(commentUid: UUID, currentUserId: Long) {
        val c = comments.findByUid(commentUid) ?: throw notFound("comment")
        try {
            commentLikes.save(BoardCommentLikeEntity(commentId = c.id, userId = currentUserId, createdAt = OffsetDateTime.now()))
            expService.grantExp(currentUserId, "LIKE_BOARD_COMMENT", c.id)
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

