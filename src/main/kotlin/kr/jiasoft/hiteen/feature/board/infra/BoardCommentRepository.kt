package kr.jiasoft.hiteen.feature.board.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.domain.BoardCommentEntity
import kr.jiasoft.hiteen.feature.board.dto.BoardCommentResponse
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
            c.id,
            c.uid,
            c.content,
            c.created_at  AS created_at,
            c.created_id  AS created_id,
            c.reply_count AS reply_count,
            (SELECT COUNT(*)::bigint FROM board_comment_likes l 
              WHERE l.comment_id = c.id) AS like_count,
            EXISTS (SELECT 1 FROM board_comment_likes l2 
                      WHERE l2.comment_id = c.id AND l2.user_id = :userId) AS liked_by_me,
            p.uid AS parent_uid
        FROM board_comments c
        LEFT JOIN board_comments p ON c.parent_id = p.id
        JOIN boards b ON b.id = c.board_id
        WHERE b.uid = :boardUid
          AND c.deleted_at IS NULL
          AND (
                (:parentUid IS NULL AND c.parent_id IS NULL)
             OR (:parentUid IS NOT NULL AND p.uid = :parentUid)
          )
          AND (:cursor IS NULL OR c.id <= (SELECT id FROM board_comments WHERE uid = :cursor))
        ORDER BY c.id DESC
        LIMIT :perPage
    """)
    fun findComments(
        boardUid: UUID,
        parentUid: UUID?,
        userId: Long,
        cursor: UUID?,
        perPage: Int
    ): Flow<BoardCommentResponse>

}