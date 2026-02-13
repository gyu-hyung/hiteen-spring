package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.infra.CountProjection
import kr.jiasoft.hiteen.feature.poll.domain.PollEntity
import kr.jiasoft.hiteen.feature.poll.dto.PollSummaryRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PollRepository : CoroutineCrudRepository<PollEntity, Long> {

    @Query("""
        SELECT created_id as id, COUNT(*)::int as count
        FROM polls
        WHERE created_id IN (:userIds) AND deleted_at IS NULL
        GROUP BY created_id
    """)
    fun countBulkByCreatedIdIn(userIds: List<Long>): Flow<CountProjection>

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
            (
                SELECT COUNT(*)::bigint
                FROM poll_comments pc
                LEFT JOIN poll_comments pp ON pp.id = pc.parent_id
                WHERE pc.poll_id = p.id
                  AND pc.deleted_at IS NULL
                  AND (pc.parent_id IS NULL OR pp.deleted_at IS NULL)
            ) AS comment_count,
            p.allow_comment,
            p.address,
            p.detail_address,
            p.lat,
            p.lng,
            CASE WHEN :userLat IS NOT NULL AND :userLng IS NOT NULL AND p.lat IS NOT NULL AND p.lng IS NOT NULL
                 THEN earth_distance(ll_to_earth(:userLat, :userLng), ll_to_earth(p.lat, p.lng))
                 ELSE NULL END AS distance,
            p.created_id,
            p.created_at,
            p.deleted_at
        FROM polls p
        LEFT JOIN (
            SELECT poll_id, COUNT(*)::bigint AS like_count
            FROM poll_likes
            GROUP BY poll_id
        ) pl_cnt ON pl_cnt.poll_id = p.id
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
          AND (:maxDistance IS NULL OR :userLat IS NULL OR :userLng IS NULL 
               OR p.lat IS NULL OR p.lng IS NULL
               OR earth_distance(ll_to_earth(:userLat, :userLng), ll_to_earth(p.lat, p.lng)) <= :maxDistance)
          AND (
              :orderType != 'DISTANCE' 
              AND (:cursor IS NULL OR p.id < :cursor)
              OR :orderType = 'DISTANCE' 
              AND (:lastDistance IS NULL OR :lastId IS NULL 
                   OR earth_distance(ll_to_earth(:userLat, :userLng), ll_to_earth(p.lat, p.lng)) > :lastDistance 
                   OR (earth_distance(ll_to_earth(:userLat, :userLng), ll_to_earth(p.lat, p.lng)) = :lastDistance AND p.id < :lastId))
          )
          AND (:authorUid IS NULL OR p.created_id = (SELECT id FROM users WHERE uid = :authorUid))
          AND p.created_id NOT IN (SELECT blocked_user_id FROM user_blocks WHERE user_id = :currentUserId)
        ORDER BY
          /* 1달 이내 데이터 우선 */
          CASE WHEN p.created_at >= NOW() - INTERVAL '1 month' THEN 0 ELSE 1 END ASC,
          /* 거리순 정렬 */
          CASE WHEN :orderType = 'DISTANCE' AND :userLat IS NOT NULL AND :userLng IS NOT NULL 
               THEN earth_distance(ll_to_earth(:userLat, :userLng), ll_to_earth(p.lat, p.lng)) END ASC NULLS LAST,
          /* orderType: LATEST(default)/POPULAR/LIKE/COMMENT */
          CASE WHEN :orderType = 'POPULAR' THEN p.vote_count END DESC NULLS LAST,
          CASE WHEN :orderType = 'COMMENT' THEN (
            SELECT COUNT(*)::bigint
            FROM poll_comments pc
            LEFT JOIN poll_comments pp ON pp.id = pc.parent_id
            WHERE pc.poll_id = p.id
              AND pc.deleted_at IS NULL
              AND (pc.parent_id IS NULL OR pp.deleted_at IS NULL)
          ) END DESC NULLS LAST,
          CASE WHEN :orderType = 'LIKE' THEN COALESCE(pl_cnt.like_count, 0) END DESC NULLS LAST,
          CASE WHEN :orderType = 'LATEST' OR :orderType IS NULL OR :orderType = '' THEN p.id END DESC NULLS LAST,
          /* fallback tie-breaker */
          p.id DESC
        LIMIT :size
    """)
    fun findSummariesByCursor(
        cursor: Long?,
        size: Int,
        currentUserId: Long?,
        type: String,
        authorUid: UUID?,
        orderType: String?,
        userLat: Double?,
        userLng: Double?,
        maxDistance: Double?,
        lastDistance: Double?,
        lastId: Long?,
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
            (
                SELECT COUNT(*)::bigint
                FROM poll_comments pc
                LEFT JOIN poll_comments pp ON pp.id = pc.parent_id
                WHERE pc.poll_id = p.id
                  AND pc.deleted_at IS NULL
                  AND (pc.parent_id IS NULL OR pp.deleted_at IS NULL)
            ) AS comment_count,
            (SELECT COUNT(*)::bigint FROM poll_likes pl WHERE pl.poll_id = p.id) AS like_count,
            EXISTS (SELECT 1 FROM poll_likes pl2 WHERE pl2.poll_id = p.id AND pl2.user_id = :currentUserId) AS liked_by_me,
            EXISTS (SELECT 1 FROM poll_users pu WHERE pu.poll_id = p.id AND pu.user_id = :currentUserId) AS voted_by_me,
            (SELECT seq FROM poll_users pu WHERE pu.poll_id = p.id AND pu.user_id = :currentUserId) AS voted_seq,
            p.allow_comment,
            p.created_id,
            p.created_at,
            p.deleted_at
        FROM polls p
        JOIN users u ON u.id = p.created_id
        WHERE p.id = :pollId
        LIMIT 1
    """)
    suspend fun findSummaryById(
        pollId: Long,
        currentUserId: Long
    ): PollSummaryRow?
}
