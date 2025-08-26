package kr.jiasoft.hiteen.feature.board.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.board.domain.BoardAssetEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardCommentEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardCommentLikeEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kr.jiasoft.hiteen.feature.board.domain.BoardLikeEntity
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentCreateRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardCreateRequest
import kr.jiasoft.hiteen.feature.board.dto.BoardDetailResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardSummaryResponse
import kr.jiasoft.hiteen.feature.board.dto.BoardUpdateRequest
import kr.jiasoft.hiteen.feature.board.infra.BoardAssetRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardCommentLikeRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardCommentRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardLikeRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
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
) {

    suspend fun getBoard(uid: UUID, currentUserId: Long?): BoardDetailResponse {
        val b = boards.findByUid(uid) ?: throw IllegalArgumentException("board not found")
        val boardId = b.id!!
        val likeCount = likes.countByBoardId(boardId)
        val commentCount = comments.countActiveByBoardId(boardId)
        val likedByMe = currentUserId?.let { likes.findByBoardIdAndUserId(boardId, it) != null } ?: false
        val attachments = boardAssetRepository.findAllByBoardId(boardId).map { it.uid }.toList().filterNotNull()
        boards.increaseHits(boardId)
        return BoardDetailResponse(
            uid = b.uid,
            category = b.category,
            subject = b.subject ?: "",
            content = b.content ?: "",
            link = b.link,
            hits = (b.hits + 1),
            assetUid = b.assetUid,
            attachments = attachments,
            startDate = b.startDate,
            endDate = b.endDate,
            status = b.status,
            address = b.address,
            detailAddress = b.detailAddress,
            createdAt = b.createdAt,
            createdId = b.createdId,
            updatedAt = b.updatedAt,
            likeCount = likeCount,
            commentCount = commentCount,
            likedByMe = likedByMe,
        )
    }

    fun listBoards(
        category: String?, q: String?, page: Int, size: Int, currentUserId: Long?
    ): Flow<BoardSummaryResponse> {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 100)
        val offset = p * s
        val uid = currentUserId ?: -1L
        return boards.searchSummaries(category, q, s, offset, uid)
            .map { row ->
                BoardSummaryResponse(
                    uid = row.uid,
                    category = row.category,
                    subject = row.subject ?: "",
                    contentPreview = (row.content ?: "").take(160),
                    link = row.link,
                    hits = row.hits ?: 0,
                    assetUid = row.assetUid,
                    createdAt = row.createdAt,
                    createdId = row.createdId,
                    likeCount = row.likeCount,
                    commentCount = row.commentCount,
                    likedByMe = row.likedByMe,
                )
            }
    }

    suspend fun createBoard(
        req: BoardCreateRequest,
        currentUserId: Long,
        files: List<FilePart>,
        ip: String?
    ): UUID {
        // 1) 파일이 있다면 먼저 업로드 → 첫 번째 파일을 대표이미지 후보로
        val uploaded: List<AssetResponse> =
            if (files.isNotEmpty()) assetService.uploadImages(files, currentUserId) else emptyList()
        val representativeUid: UUID? = uploaded.firstOrNull()?.uid

        // 2) 대표이미지(assetUid)를 반영해 게시글 생성
        val saved = boards.save(
            BoardEntity(
                category = req.category,
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
                createdId = currentUserId,
            )
        )

        // 3) 업로드된 모든 파일을 board_assets 매핑에 저장
        if (uploaded.isNotEmpty()) {
            uploaded.forEach { a ->
                boardAssetRepository.save(
                    BoardAssetEntity(
                        boardId = saved.id!!,
                        uid = a.uid
                    )
                )
            }

        }

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
    suspend fun updateBoard(
        uid: UUID,
        req: BoardUpdateRequest,
        currentUserId: Long,
        files: List<FilePart> = emptyList(),
        ip: String? = null,
        replaceAssets: Boolean? = false,
        deleteAssetUids: List<UUID>?
    ) {
        val b = boards.findByUid(uid) ?: throw notFound("board")
        val boardId = b.id ?: error("Board id not generated")

        // 1) 에셋 매핑 삭제
        val toDelete = deleteAssetUids?.distinct().orEmpty()
        if (replaceAssets == true) {
            boardAssetRepository.deleteByBoardId(boardId)
        } else if (toDelete.isNotEmpty()) {
            boardAssetRepository.deleteByBoardIdAndUidIn(boardId, toDelete)
        }

        // 2) 파일 업로드 -> 매핑 추가 + 대표이미지 후보(첫 번째 업로드)
//        var representativeUidFromFiles: UUID? = null
        if (files.isNotEmpty()) {
            val uploadedFiles = assetService.uploadImages(files, currentUserId)
            uploadedFiles.forEachIndexed { idx, a ->
//                if (idx == 0) representativeUidFromFiles = a.uid   // 첫 번째 파일을 대표이미지로
                boardAssetRepository.save(
                    BoardAssetEntity(
                        boardId = boardId,
                        uid = a.uid,
                    )
                )
            }
        }

//        // 3) 대표 이미지 결정
//        val currentRep = b.assetUid
//        val repDeletedByReplace = (replaceAssets == true)
//        val repDeletedByPartial = (!repDeletedByReplace && currentRep != null && toDelete.contains(currentRep))
//
//        val newAssetUid: UUID? = when {
//            // 업로드 파일이 있으면 무조건 그 첫 번째로
//            representativeUidFromFiles != null -> representativeUidFromFiles
//
//            // 전체 교체인데 업로드가 없으면 대표 제거
//            repDeletedByReplace && files.isEmpty() -> null
//
//            // 부분 삭제로 기존 대표가 지워졌고 업로드도 없으면 남은 자산 중 하나로 대체 선택
//            repDeletedByPartial && files.isEmpty() -> {
//                boardAssetRepository.findTopUidByBoardIdOrderByIdDesc(boardId)
//            }
//
//            // 그 외에는 기존 대표 유지
//            else -> currentRep
//        }

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


    //    @Transactional
    suspend fun softDeleteBoard(uid: UUID, currentUserId: Long) {
        val b = boards.findByUid(uid) ?: throw notFound("board")
        val updated = b.copy(deletedId = currentUserId, deletedAt = OffsetDateTime.now())
        boards.save(updated)
    }

    // ---------------------- Likes ----------------------
//    @Transactional
    suspend fun likeBoard(uid: UUID, currentUserId: Long) {
        val b = boards.findByUid(uid) ?: throw notFound("board")
        try {
            likes.save(BoardLikeEntity(boardId = b.id!!, userId = currentUserId))
        } catch (_: DuplicateKeyException) {
        }
    }

    //    @Transactional
    suspend fun unlikeBoard(uid: UUID, currentUserId: Long) {
        val b = boards.findByUid(uid) ?: throw notFound("board")
        likes.deleteByBoardIdAndUserId(b.id!!, currentUserId)
    }

    // ---------------------- Comments ----------------------
//    @Transactional
    suspend fun createComment(boardUid: UUID, req: BoardCommentCreateRequest, currentUserId: Long): UUID {
        val b = boards.findByUid(boardUid) ?: throw notFound("board")
        val parent: BoardCommentEntity? = req.parentUid?.let { comments.findByUid(it) }
        val saved = comments.save(
            BoardCommentEntity(
                boardId = b.id!!,
                parentId = parent?.id,
                content = req.content,
                createdId = currentUserId,
            )
        )
        if (parent != null) comments.increaseReplyCount(parent.id!!)
        return saved.uid
    }

    fun listTopComments(boardUid: UUID, currentUserId: Long?): Flow<BoardCommentResponse> =
        comments.findTopCommentRows(boardUid, (currentUserId ?: -1L)).map { row ->
            BoardCommentResponse(
                uid = row.uid,
                content = row.content ?: "",
                createdAt = row.createdAt,
                createdId = row.createdId,
                replyCount = row.replyCount,
                likeCount = row.likeCount,
                likedByMe = row.likedByMe,
                parentUid = null,
            )
        }

    fun listReplies(parentCommentUid: UUID, currentUserId: Long?): Flow<BoardCommentResponse> =
        comments.findReplyRows(parentCommentUid, (currentUserId ?: -1L)).map { row ->
            BoardCommentResponse(
                uid = row.uid,
                content = row.content ?: "",
                createdAt = row.createdAt,
                createdId = row.createdId,
                replyCount = row.replyCount,
                likeCount = row.likeCount,
                likedByMe = row.likedByMe,
                parentUid = row.parentUid,
            )
        }

    //    @Transactional
    suspend fun likeComment(commentUid: UUID, currentUserId: Long) {
        val c = comments.findByUid(commentUid) ?: throw notFound("comment")
        try {
            commentLikes.save(BoardCommentLikeEntity(commentId = c.id!!, userId = currentUserId))
        } catch (_: DuplicateKeyException) {
        }
    }


    //    @Transactional
    suspend fun unlikeComment(commentUid: UUID, currentUserId: Long) {
        val c = comments.findByUid(commentUid) ?: throw notFound("comment")
        commentLikes.deleteByCommentIdAndUserId(c.id!!, currentUserId)
    }


    private fun notFound(what: String) = ResponseStatusException(HttpStatus.NOT_FOUND, "$what not found")

}