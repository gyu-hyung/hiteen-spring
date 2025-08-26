package kr.jiasoft.hiteen.feature.board.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.domain.BoardCommentEntity
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BoardCommentRepository : CoroutineCrudRepository<BoardCommentEntity, Long> {
    suspend fun findByUid(uid: UUID): BoardCommentEntity?
    fun findAllByBoardIdAndParentIdIsNullOrderByIdAsc(boardId: Long): Flow<BoardCommentEntity>
    fun findAllByParentIdOrderByIdAsc(parentId: Long): Flow<BoardCommentEntity>

    @Query("SELECT COUNT(*) FROM board_comments WHERE board_id = :boardId AND deleted_at IS NULL")
    suspend fun countActiveByBoardId(boardId: Long): Long

    @Query("UPDATE board_comments SET reply_count = reply_count + 1 WHERE id = :parentId")
    suspend fun increaseReplyCount(parentId: Long): Int

    @Query("""
        SELECT c.uid,
               c.content,
               c.created_at AS createdAt,
               c.created_id AS createdId,
               c.reply_count AS replyCount,
               (SELECT COUNT(*) FROM board_comment_likes l WHERE l.comment_id = c.id) AS likeCount,
               EXISTS (SELECT 1 FROM board_comment_likes l2 WHERE l2.comment_id = c.id AND l2.user_id = :userId) AS likedByMe
        FROM board_comments c
        JOIN boards b ON b.id = c.board_id
        WHERE b.uid = :boardUid AND c.deleted_at IS NULL AND c.parent_id IS NULL
        ORDER BY c.id ASC
    """)
    fun findTopCommentRows(boardUid: UUID, userId: Long): Flow<BoardCommentRow>

    @Query("""
        SELECT c.uid,
               c.content,
               c.created_at AS createdAt,
               c.created_id AS createdId,
               c.reply_count AS replyCount,
               (SELECT COUNT(*) FROM board_comment_likes l WHERE l.comment_id = c.id) AS likeCount,
               EXISTS (SELECT 1 FROM board_comment_likes l2 WHERE l2.comment_id = c.id AND l2.user_id = :userId) AS likedByMe,
               p.uid AS parentUid
        FROM board_comments c
        JOIN board_comments p ON c.parent_id = p.id
        WHERE p.uid = :parentUid AND c.deleted_at IS NULL
        ORDER BY c.id ASC
    """)
    fun findReplyRows(parentUid: UUID, userId: Long): Flow<BoardCommentRow>
}