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

    @Query("UPDATE board_comments SET reply_count = GREATEST(reply_count - 1, 0) WHERE id = :parentId")
    suspend fun decreaseReplyCount(parentId: Long): Int?

    @Query("""
        SELECT
            c.uid,
            c.content,
            c.created_at  AS created_at,
            c.created_id  AS created_id,
            c.reply_count AS reply_count,
            (SELECT COUNT(*)::bigint FROM board_comment_likes l 
               WHERE l.comment_id = c.id) AS like_count,
            EXISTS (SELECT 1 FROM board_comment_likes l2 
                      WHERE l2.comment_id = c.id AND l2.user_id = :userId) AS liked_by_me,
            NULL::uuid AS parent_uid     -- DTO의 parentUid(UUID?)와 타입 일치
        FROM board_comments c
        JOIN boards b ON b.id = c.board_id
        WHERE b.uid = :boardUid
          AND c.deleted_at IS NULL
          AND c.parent_id IS NULL
        ORDER BY c.id DESC
    """)
    fun findTopCommentRows(boardUid: UUID, userId: Long): Flow<BoardCommentRow>

    @Query("""
        SELECT
            c.uid,
            c.content,
            c.created_at  AS created_at,
            c.created_id  AS created_id,
            c.reply_count AS reply_count,
            (SELECT COUNT(*)::bigint 
               FROM board_comment_likes l 
              WHERE l.comment_id = c.id)                 AS like_count,
            EXISTS (SELECT 1 
                      FROM board_comment_likes l2 
                     WHERE l2.comment_id = c.id 
                       AND l2.user_id = :userId)         AS liked_by_me,
            p.uid                                         AS parent_uid
        FROM board_comments c
        JOIN board_comments p ON c.parent_id = p.id
        WHERE p.uid = :parentUid
          AND c.deleted_at IS NULL
          -- AND p.deleted_at IS NULL
        ORDER BY c.id DESC
    """)
    fun findReplyRows(parentUid: UUID, userId: Long): Flow<BoardCommentRow>
}