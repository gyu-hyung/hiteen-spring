package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminCommentResponse
import kr.jiasoft.hiteen.feature.board.domain.BoardCommentEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminBoardCommentRepository : CoroutineCrudRepository<BoardCommentEntity, Long> {
    @Query("""
        SELECT
            c.id,
            c.board_id        AS fk_id,
            c.uid,
            c.parent_id,
            c.reply_count,
            c.report_count,
            (SELECT COUNT(*) FROM board_comment_likes bcl WHERE bcl.comment_id = c.id) AS like_count,
            u.nickname,
            c.content,
            c.created_at,
            CASE
                WHEN c.deleted_at IS NOT NULL THEN 'DELETED'
                ELSE 'ACTIVE'
            END AS status,
            b.content AS board_content,
            'BOARD' AS type
        FROM board_comments c
        JOIN boards b ON b.id = c.board_id
        JOIN users u ON u.id = c.created_id
        WHERE
            (
                :search IS NULL
                OR (
                    CASE
                        WHEN :searchType = 'ALL' THEN
                            c.content ILIKE '%' || :search || '%'
                            OR u.nickname ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'content' THEN
                            c.content ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'boardContent' THEN
                            b.content ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'nickname' THEN
                            u.nickname ILIKE '%' || :search || '%'
                    END
                )
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
//        fkId: Long?,     // board_id
        uid: UUID?,
    ): Flow<AdminCommentResponse>

    @Query("""
        SELECT COUNT(*)
        FROM board_comments c
        JOIN boards b ON b.id = c.board_id
        JOIN users u ON u.id = c.created_id
        WHERE
            (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        c.content ILIKE '%' || :search || '%'
                        OR u.nickname ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'content' THEN
                        c.content ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'boardContent' THEN
                        b.content ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
            
                    ELSE TRUE
                END
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
//        fkId: Long?,
        uid: UUID?,
    ): Int
}

