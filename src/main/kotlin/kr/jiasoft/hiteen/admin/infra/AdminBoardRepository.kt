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
            (SELECT COUNT(*) FROM board_comments bc WHERE bc.board_id = b.id AND bc.deleted_at IS NULL) AS comment_count,
            (SELECT COUNT(*) FROM board_likes bl WHERE bl.board_id = b.id) AS like_count,
            ( SELECT u.uid FROM users u WHERE u.id = b.created_id ) AS created_uid, 
            ( SELECT u.nickname FROM users u WHERE u.id = b.created_id ) AS nickname
        FROM boards b
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        b.subject ILIKE CONCAT('%', :search, '%')
                        OR b.content ILIKE CONCAT('%', :search, '%')
                        OR (
                            SELECT u.nickname
                            FROM users u
                            WHERE u.id = b.created_id
                        ) ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'subject' AND b.subject ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'content' AND b.content ILIKE CONCAT('%', :search, '%'))
                OR (
                    :searchType = 'nickname'
                    AND (
                        SELECT u.nickname
                        FROM users u
                        WHERE u.id = b.created_id
                    ) ILIKE CONCAT('%', :search, '%')
                )
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND b.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND b.status = 'INACTIVE')
            )
    
            AND (
                :category IS NULL OR :category = 'ALL'
                OR (:category = 'POST' AND b.category = 'POST')
                OR (:category = 'NOTICE' AND b.category = 'NOTICE')
                OR (:category = 'EVENT' AND b.category = 'EVENT')
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = b.created_id ) = :uid
            )

    
        ORDER BY
            CASE WHEN :order = 'DESC' THEN b.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN b.created_at END ASC
    
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
        category: String?,
    ): Flow<AdminBoardListResponse>




    @Query("""
        SELECT COUNT(*)
        FROM boards b
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        b.subject ILIKE CONCAT('%', :search, '%')
                        OR b.content ILIKE CONCAT('%', :search, '%')
                        OR (
                            SELECT u.nickname
                            FROM users u
                            WHERE u.id = b.created_id
                        ) ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'subject' AND b.subject ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'content' AND b.content ILIKE CONCAT('%', :search, '%'))
                OR (
                    :searchType = 'nickname'
                    AND (
                        SELECT u.nickname
                        FROM users u
                        WHERE u.id = b.created_id
                    ) ILIKE CONCAT('%', :search, '%')
                )
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND b.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND b.status = 'INACTIVE')
            )
    
            AND (
                :category IS NULL OR :category = 'ALL'
                OR (:category = 'POST' AND b.category = 'POST')
                OR (:category = 'NOTICE' AND b.category = 'NOTICE')
                OR (:category = 'EVENT' AND b.category = 'EVENT')
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = b.created_id ) = :uid
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        category: String?,
    ): Int

}
