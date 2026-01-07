package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminPinResponse
import kr.jiasoft.hiteen.admin.dto.AdminPollResponse
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
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



}

