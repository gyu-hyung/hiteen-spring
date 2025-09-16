package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.domain.PollEntity
import kr.jiasoft.hiteen.feature.poll.dto.PollSummaryRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollRepository : CoroutineCrudRepository<PollEntity, Long> {

    @Query("UPDATE polls SET vote_count = vote_count + 1 WHERE id = :id")
    suspend fun increaseVoteCount(id: Long): Int


    @Query("""
        SELECT 
            p.id,
            p.question,
            p.photo,
            p.selects::text AS selects,
            p.color_start,
            p.color_end,
            p.vote_count,
            p.comment_count,
--          u.username,
--          u.nickname,
--          u.asset_uid AS profile_image,
            p.created_id,
            p.created_at,
            EXISTS (SELECT seq FROM poll_users pu WHERE pu.poll_id = p.id AND pu.user_id = :currentUserId) AS voted_by_me,
            (SELECT seq FROM poll_users pu WHERE pu.poll_id = p.id AND pu.user_id = :currentUserId) AS voted_seq
        FROM polls p
        JOIN users u ON u.id = p.created_id
        WHERE p.deleted_at IS NULL
          AND (:cursor IS NULL OR p.id < :cursor)
        ORDER BY p.id DESC
        LIMIT :size
    """)
    fun findSummariesByCursor(
        cursor: Long?,
        size: Int,
        currentUserId: Long?
    ): Flow<PollSummaryRow>

}






















