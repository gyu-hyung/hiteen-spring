package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.GameEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminGameRepository : CoroutineCrudRepository<GameEntity, Long> {

    /**
     * 🔹 전체 개수 조회
     */
    @Query("""
        SELECT COUNT(*)
        FROM games g
        WHERE
            (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        g.name ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'name' THEN
                        g.name ILIKE '%' || :search || '%'
                    
                    ELSE TRUE
                END
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND g.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND g.status = 'INACTIVE')
            )
            
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
    ): Int



    /**
     * 🔹 페이징 조회 (AdminFriendResponse)
     */
    @Query("""
        SELECT g.*
        FROM games g
        WHERE
            (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        g.name ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'name' THEN
                        g.name ILIKE '%' || :search || '%'
                    
                    ELSE TRUE
                END
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND g.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND g.status = 'INACTIVE')
            )

        ORDER BY g.status DESC, 
            CASE WHEN :order = 'DESC' THEN g.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN g.created_at END ASC
            
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
    ): Flow<GameEntity?>


}