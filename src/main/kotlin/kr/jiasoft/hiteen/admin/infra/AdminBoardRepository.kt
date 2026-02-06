package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminBoardListResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminBoardRepository : CoroutineCrudRepository<BoardEntity, Long> {

    @Query("""
        SELECT 
            b.*,
            u.uid AS created_uid, 
            u.nickname AS nickname,
            (SELECT COUNT(*) FROM board_comments bc WHERE bc.board_id = b.id AND bc.deleted_at IS NULL) AS comment_count,
            (SELECT COUNT(*) FROM board_likes bl WHERE bl.board_id = b.id) AS like_count
        FROM boards b
        LEFT JOIN users u ON u.id = b.created_id
        WHERE
            b.deleted_at IS NULL
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        b.subject ILIKE '%' || :search || '%'
                        OR b.content ILIKE '%' || :search || '%'
                        OR u.nickname ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'subject' THEN
                        b.subject ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'content' THEN
                        b.content ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
            
                    ELSE TRUE
                END
            ) 

            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND b.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND b.status = 'INACTIVE')
            )
    
            AND (
                :displayStatus IS NULL OR :displayStatus = 'ALL'
                OR (
                    :displayStatus = 'ACTIVE'
                    AND (b.start_date IS NULL OR b.start_date <= CURRENT_DATE)
                    AND (b.end_date IS NULL OR b.end_date >= CURRENT_DATE)
                )
                OR (
                    :displayStatus = 'INACTIVE'
                    AND (
                        (b.start_date IS NOT NULL AND b.start_date > CURRENT_DATE)
                        OR (b.end_date IS NOT NULL AND b.end_date < CURRENT_DATE)
                    )
                )
            )
    
            AND (
                :category IS NULL OR :category = 'ALL'
                OR (:category = 'POST' AND b.category = 'POST')
                OR (:category = 'NOTICE' AND b.category = 'NOTICE')
                OR (
                    :category = 'EVENT'
                    AND b.category IN ('EVENT', 'EVENT_WINNING')
                )
                OR (:category = 'EVENT_WINNING' AND b.category = 'EVENT_WINNING')
            )
    
            AND (
                :uid IS NULL
                OR u.uid = :uid
            )
        ORDER BY
            CASE WHEN :order = 'DESC' THEN b.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN b.created_at END ASC
    
        LIMIT :size OFFSET GREATEST((:page - 1) * :size, 0)
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        displayStatus: String?,
        uid: UUID?,
        category: String?,
    ): Flow<AdminBoardListResponse>

    @Query("""
        SELECT COUNT(*)
        FROM boards b
        LEFT JOIN users u ON u.id = b.created_id
        WHERE
            b.deleted_at IS NULL
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        b.subject ILIKE '%' || :search || '%'
                        OR b.content ILIKE '%' || :search || '%'
                        OR u.nickname ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'subject' THEN
                        b.subject ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'content' THEN
                        b.content ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
            
                    ELSE TRUE
                END
            ) 
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND b.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND b.status = 'INACTIVE')
            )
    
            AND (
                :displayStatus IS NULL OR :displayStatus = 'ALL'
                OR (
                    :displayStatus = 'ACTIVE'
                    AND (b.start_date IS NULL OR b.start_date <= CURRENT_DATE)
                    AND (b.end_date IS NULL OR b.end_date >= CURRENT_DATE)
                )
                OR (
                    :displayStatus = 'INACTIVE'
                    AND (
                        (b.start_date IS NOT NULL AND b.start_date > CURRENT_DATE)
                        OR (b.end_date IS NOT NULL AND b.end_date < CURRENT_DATE)
                    )
                )
            )
    
            AND (
                :category IS NULL OR :category = 'ALL'
                OR (:category = 'POST' AND b.category = 'POST')
                OR (:category = 'NOTICE' AND b.category = 'NOTICE')
                OR (
                    :category = 'EVENT'
                    AND b.category IN ('EVENT', 'EVENT_WINNING')
                )
                OR (:category = 'EVENT_WINNING' AND b.category = 'EVENT_WINNING')
            )
    
            AND (
                :uid IS NULL
                OR u.uid = :uid
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        displayStatus: String?,
        uid: UUID?,
        category: String?,
    ): Int

}
