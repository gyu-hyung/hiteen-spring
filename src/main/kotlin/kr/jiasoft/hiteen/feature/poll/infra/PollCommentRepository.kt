package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.domain.PollCommentEntity
import kr.jiasoft.hiteen.feature.poll.dto.PollCommentResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PollCommentRepository : CoroutineCrudRepository<PollCommentEntity, Long> {

    suspend fun findByUid(uid: UUID): PollCommentEntity?

    @Query("UPDATE poll_comments SET reply_count = reply_count + 1 WHERE id = :parentId")
    suspend fun increaseReplyCount(parentId: Long): Int

    @Query("UPDATE poll_comments SET reply_count = GREATEST(reply_count - 1, 0) WHERE id = :parentId")
    suspend fun decreaseReplyCount(parentId: Long): Int?


    @Query("""
        SELECT
            c.id,
            c.uid,
            c.content,
            c.created_at  AS created_at,
            c.created_id  AS created_id,
            c.reply_count AS reply_count,
            (SELECT COUNT(*)::bigint FROM poll_comment_likes l WHERE l.comment_id = c.id) AS like_count,
            EXISTS (SELECT 1 FROM poll_comment_likes l2 WHERE l2.comment_id = c.id AND l2.user_id = :userId) AS liked_by_me,
            p.uid AS parent_uid
        FROM poll_comments c
        LEFT JOIN poll_comments p ON c.parent_id = p.id
        JOIN polls b ON b.id = c.poll_id
        WHERE b.id = :pollId
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
        pollId: Long,
        parentUid: UUID?,
        userId: Long,
        cursor: UUID?,
        perPage: Int
    ): Flow<PollCommentResponse>



}