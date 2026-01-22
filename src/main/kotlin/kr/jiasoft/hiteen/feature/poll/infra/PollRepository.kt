package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.domain.PollEntity
import kr.jiasoft.hiteen.feature.poll.dto.PollSummaryRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PollRepository : CoroutineCrudRepository<PollEntity, Long> {

    @Query("UPDATE polls SET vote_count = vote_count + 1 WHERE id = :id")
    suspend fun increaseVoteCount(id: Long): Int


    @Query("UPDATE polls SET comment_count = comment_count + 1 WHERE id = :id")
    suspend fun increaseCommentCount(id: Long): Int


    @Query("UPDATE polls SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = :id")
    suspend fun decreaseCommentCount(id: Long): Int


    @Query("""
        SELECT 
            p.id,
            p.question,
            p.color_start,
            p.color_end,
            p.vote_count,
            p.comment_count,
            p.allow_comment,
            p.created_id,
            p.created_at
        FROM polls p
        WHERE p.deleted_at IS NULL
          AND p.status = 'ACTIVE'
          AND (
              :type = 'all'
              OR (:type = 'mine' AND p.created_id = :currentUserId)
              OR (:type = 'participated' AND (
                    p.id IN (SELECT poll_id FROM poll_users WHERE user_id = :currentUserId)
                    OR p.id IN (SELECT poll_id FROM poll_comments WHERE created_id = :currentUserId AND deleted_at IS NULL)
              ))
          )
          AND (:cursor IS NULL OR p.id < :cursor)
          AND (:authorUid IS NULL OR p.created_id = (SELECT id FROM users WHERE uid = :authorUid))
        ORDER BY p.id DESC
        LIMIT :size
    """)
    fun findSummariesByCursor(
        cursor: Long?,
        size: Int,
        currentUserId: Long?,
        type: String,
        authorUid: UUID?
    ): Flow<PollSummaryRow>


    @Query("""
        SELECT 
            p.id,
            p.question,
            p.photo,
            p.selects::text AS selects,
            p.color_start,
            p.color_end,
            p.vote_count,
            (SELECT COUNT(*)::bigint FROM poll_comments pc WHERE pc.poll_id = p.id AND pc.deleted_at IS NULL) AS comment_count,
            (SELECT COUNT(*)::bigint FROM poll_likes pl WHERE pl.poll_id = p.id) AS like_count,
            EXISTS (SELECT 1 FROM poll_likes pl2 WHERE pl2.poll_id = p.id AND pl2.user_id = :currentUserId) AS liked_by_me,
            EXISTS (SELECT 1 FROM poll_users pu WHERE pu.poll_id = p.id AND pu.user_id = :currentUserId) AS voted_by_me,
            (SELECT seq FROM poll_users pu WHERE pu.poll_id = p.id AND pu.user_id = :currentUserId) AS voted_seq,
            p.allow_comment,
            p.created_id,
            p.created_at
        FROM polls p
        JOIN users u ON u.id = p.created_id
        WHERE p.deleted_at IS NULL
          AND p.id = :pollId
        LIMIT 1
    """)
    suspend fun findSummaryById(
        pollId: Long,
        currentUserId: Long
    ): PollSummaryRow?



}






















