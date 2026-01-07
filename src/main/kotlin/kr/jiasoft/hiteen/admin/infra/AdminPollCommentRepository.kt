package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminCommentResponse
import kr.jiasoft.hiteen.feature.poll.domain.PollCommentEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminPollCommentRepository : CoroutineCrudRepository<PollCommentEntity, Long> {


    @Query("""
        SELECT
            c.id,
            c.poll_id         AS fk_id,
            c.uid,
            c.parent_id,
            c.reply_count,
            c.report_count,
            (SELECT COUNT(*) FROM poll_comment_likes pcl WHERE pcl.comment_id = c.id) AS like_count,
            u.nickname,
            c.content,
            c.created_at,
            CASE
                WHEN c.deleted_at IS NOT NULL THEN 'DELETED'
                ELSE 'ACTIVE'
            END AS status,
            p.question AS board_content,
            'POLL' AS type
        FROM poll_comments c
        JOIN polls p ON p.id = c.poll_id
        JOIN users u ON u.id = c.created_id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        c.content ILIKE CONCAT('%', :search, '%')
                        OR u.nickname ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'content' AND c.content ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'boardContent' AND p.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
            )
            
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND c.deleted_at IS NULL)
                OR (:status = 'DELETED' AND c.deleted_at IS NOT NULL)
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = c.created_id ) = :uid
            )
    
        ORDER BY
            CASE WHEN :order = 'DESC' THEN c.created_at END DESC,
            CASE WHEN :order = 'ASC'  THEN c.created_at END ASC
    
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
    ): Flow<AdminCommentResponse>





    @Query("""
        SELECT COUNT(*)
        FROM poll_comments c
        JOIN polls p ON p.id = c.poll_id
        JOIN users u ON u.id = c.created_id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        c.content ILIKE CONCAT('%', :search, '%')
                        OR u.nickname ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'content' AND c.content ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'boardContent' AND p.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
            )
            
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND c.deleted_at IS NULL)
                OR (:status = 'DELETED' AND c.deleted_at IS NOT NULL)
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = c.created_id ) = :uid
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
    ): Int




}

