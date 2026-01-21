package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminPollDetailRow
import kr.jiasoft.hiteen.admin.dto.AdminPollResponse
import kr.jiasoft.hiteen.feature.poll.domain.PollEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminPollRepository : CoroutineCrudRepository<PollEntity, Long> {



    @Query("""
        SELECT
            p.id,
            p.question,
            p.photo,
    
            p.vote_count,
            p.comment_count,
            (SELECT COUNT(*) FROM poll_likes pl WHERE pl.poll_id = p.id) AS like_count,
            p.report_count,
            p.allow_comment,
    
            p.status AS status,
    
            u.uid      AS user_uid,
            u.nickname AS nickname,
    
            p.created_at
    
        FROM polls p
        LEFT JOIN users u ON u.id = p.created_id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        p.question ILIKE CONCAT('%', :search, '%')
                        OR u.nickname ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'question' AND p.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR p.status = :status
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = p.created_id ) = :uid
            )
    
        ORDER BY
            CASE WHEN :order = 'DESC' THEN p.created_at END DESC,
            CASE WHEN :order = 'ASC'  THEN p.created_at END ASC
    
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,        // polls.status
        uid: UUID?,
    ): Flow<AdminPollResponse>




    @Query("""
        SELECT COUNT(*)
        FROM polls p
        LEFT JOIN users u ON u.id = p.created_id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        p.question ILIKE CONCAT('%', :search, '%')
                        OR u.nickname ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'question' AND p.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR p.status = :status
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = p.created_id ) = :uid
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
    ): Int


    // --- Admin 상세 조회용 쿼리 추가 ---
    @Query("""
        SELECT
            p.id,
            p.question,
            p.photo,
            p.vote_count,
            (SELECT COUNT(*)::bigint FROM poll_comments pc WHERE pc.poll_id = p.id AND pc.deleted_at IS NULL) AS comment_count,
            (SELECT COUNT(*)::bigint FROM poll_likes pl WHERE pl.poll_id = p.id) AS like_count,
            p.report_count,
            p.allow_comment,
            (
                SELECT jsonb_agg(row_to_json(r))
                FROM (
                    SELECT ps.id, ps.seq, ps.content, ps.vote_count,
                        (
                            SELECT psp.asset_uid
                            FROM poll_select_photos psp
                            WHERE psp.select_id = ps.id
                            ORDER BY psp.seq ASC, psp.id ASC
                            LIMIT 1
                        ) AS photo
                    FROM poll_selects ps
                    WHERE ps.poll_id = p.id
                    ORDER BY ps.seq ASC
                ) r
            ) AS selects,
            (
                SELECT ARRAY_AGG(ps.content ORDER BY ps.seq ASC) FROM poll_selects ps WHERE ps.poll_id = p.id
            ) AS options,
            NULL::text AS start_date,
            NULL::text AS end_date,
            p.status AS status,
            u.nickname AS nickname,
            p.created_at
        FROM polls p
        JOIN users u ON u.id = p.created_id
        WHERE p.id = :id
        LIMIT 1
    """)
    suspend fun findDetailById(id: Long): AdminPollDetailRow?



}
