package kr.jiasoft.hiteen.feature.board.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.board.domain.BoardAssetEntity
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
import kr.jiasoft.hiteen.feature.board.infra.BoardCommentLikeRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardCommentRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardLikeRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.buildPushData
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.util.UUID

@Service
class BoardService(
    private val boards: BoardRepository,
    private val boardAssetRepository: BoardAssetRepository,
    private val comments: BoardCommentRepository,
    private val likes: BoardLikeRepository,
    private val commentLikes: BoardCommentLikeRepository,
    private val assetService: AssetService,
    private val userService: UserService,
    private val expService: ExpService,
    private val pointService: PointService,
    private val pushService: PushService,

    private val followRepository: FollowRepository,
) {


    suspend fun getBoard(uid: UUID, currentUserId: Long?): BoardResponse {
        val userId = currentUserId ?: -1L
        val b = boards.findDetailByUid(uid, userId) ?: throw IllegalArgumentException("board not found")
        val userSummary = userService.findUserSummary(b.createdId)

        val perPage = 15
        val commentList = comments.findComments(b.uid, null, userId, null, perPage + 1)
                .map { comments ->
                    comments.copy(
                        user = userService.findUserSummary(comments.createdId)
                    )
                }.toList()

        val hasMore = commentList.size > perPage
        val items = if (hasMore) commentList.dropLast(1) else commentList
        val nextCursor = if (hasMore) commentList.lastOrNull()?.uid?.toString() else null

        // 조회수 증가
        b.id.let { boards.increaseHits(it) }


        // 공지사항/이벤트 확인 시 경험치 부여
        if(b.category == "NOTICE" || b.category == "EVENT") {
            expService.grantExp(userId, "NOTICE_READ", b.id)
        }

        return b.copy(
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
        followOnly: Boolean, friendOnly: Boolean, sameSchoolOnly: Boolean
    ): ApiPage<BoardResponse> {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 100)
        val offset = p * s
        val uid = currentUserId ?: -1L

        // 총 개수
        val total = boards.countSearchResults(category, q, uid, followOnly, friendOnly, sameSchoolOnly)
        val lastPage = if (total == 0) 0 else (total - 1) / s

        val rows = boards.searchSummariesByPage(category, q, s, offset, uid, followOnly, friendOnly, sameSchoolOnly)
            .map { row ->
                row.copy(
                    subject = row.subject,
                    content = row.content.take(160),
                    user = userService.findUserSummary(row.createdId),
                    attachments = boardAssetRepository.findAllByBoardId(row.id)?.map { it.uid }
                )
            }
            .toList()

        return ApiPage(
            total = total,
            lastPage = lastPage,
            items = rows,
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
        return ApiPageCursor(
            nextCursor = nextCursor,
            items = items.map { row ->
                row.copy(
                    subject = row.subject,
                    content = row.content.take(160),
                    user = userService.findUserSummary(row.createdId),
                    attachments = boardAssetRepository.findAllByBoardId(row.id)?.map { it.uid }
                )
            },
            perPage = s
        )
    }



    suspend fun create(
        req: BoardCreateRequest,
        user: UserEntity,
        files: List<FilePart>,
        ip: String?
    ): UUID {
        // 1) 파일이 있다면 먼저 업로드 → 첫 번째 파일을 대표이미지 후보로
        val uploaded: List<AssetResponse> =
            if (files.isNotEmpty()) assetService.uploadImages(files, user.id, AssetCategory.POST) else emptyList()
        val representativeUid: UUID? = uploaded.firstOrNull()?.uid

        // 2) 대표이미지(assetUid)를 반영해 게시글 생성
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
                assetUid = representativeUid,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )

        // 3) 업로드된 모든 파일을 board_assets 매핑에 저장
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

        //경험치
        expService.grantExp(user.id, "CREATE_BOARD", saved.id)
        //포인트
        pointService.applyPolicy(user.id, PointPolicy.STORY_POST, saved.id)
        //포스팅 알림
        val followerIds = followRepository.findAllFollowerIds(user.id).toList()
        pushService.sendAndSavePush(followerIds, PushTemplate.NEW_POST.buildPushData("nickname" to user))

        return saved.uid
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
        if (files.isNotEmpty()) {
            val uploadedFiles = assetService.uploadImages(files, currentUserId, AssetCategory.POST)
            uploadedFiles.forEach { a ->
                boardAssetRepository.save(
                    BoardAssetEntity(
                        boardId = boardId,
                        uid = a.uid,
                    )
                )
            }
        }

        // 4) 본문/메타 갱신 저장
        val merged = b.copy(
            category = req.category ?: b.category,
            subject = req.subject ?: b.subject,
            content = req.content ?: b.content,
            link = req.link ?: b.link,
//            assetUid = newAssetUid,
            startDate = req.startDate ?: b.startDate,
            endDate = req.endDate ?: b.endDate,
            status = req.status ?: b.status,
            address = req.address ?: b.address,
            detailAddress = req.detailAddress ?: b.detailAddress,
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
    ): List<BoardCommentResponse>
            = comments.findComments(boardUid, parentUid, currentUserId ?: -1L, cursor, perPage)
                .map { comments ->
                    comments.copy(
                        user = userService.findUserSummary(comments.createdId)
                    )
                }.toList()


    suspend fun listMyComments(
        userId: Long,
        cursor: UUID?,
        perPage: Int
    ): List<BoardCommentResponse> =
        comments.findMyComments(userId, cursor, perPage)
            .map { comment ->
                comment.copy(
                    user = userService.findUserSummary(comment.createdId)
                )
            }.toList()


    suspend fun createComment(boardUid: UUID, req: BoardCommentRegisterRequest, user: UserEntity): UUID {
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
        if (parent != null) comments.increaseReplyCount(parent.id)

        expService.grantExp(user.id, "CREATE_BOARD_COMMENT", saved.id)
        pointService.applyPolicy(user.id, PointPolicy.STORY_COMMENT, saved.id)
        pushService.sendAndSavePush(listOf(b.createdId), PushTemplate.BOARD_COMMENT.buildPushData("nickname" to user.nickname))
        return saved.uid
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