package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminPlayResponse
import kr.jiasoft.hiteen.feature.play.domain.GameHistoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminPlayRepository : CoroutineCrudRepository<GameHistoryEntity, Long> {



    @Query("""
        SELECT 
            gh.*,
            (SELECT nickname FROM users u WHERE u.id = sp.user_id) AS nickname,
            (SELECT (year % 100)::text || '-' || month::text || '-' || round::text FROM seasons s WHERE s.id = gh.season_id) AS season_no,
            (SELECT name FROM games g WHERE g.id = gh.game_id) AS game_name
        FROM game_history gh
        LEFT JOIN season_participants sp ON sp.id = gh.participant_id 
        LEFT JOIN users u ON sp.user_id = u.id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        SELECT u.nickname
                        FROM users u
                        WHERE u.id = sp.user_id
                    ) ILIKE CONCAT('%', :search, '%')
                )
                OR (
                    :searchType = 'nickname'
                    AND (
                        SELECT u.nickname
                        FROM users u
                        WHERE u.id = sp.user_id
                    ) ILIKE CONCAT('%', :search, '%')
                )
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'PLAYING' AND gh.status = 'PLAYING')
                OR (:status = 'DONE' AND gh.status = 'DONE')
            )
    
            AND (
                :uid IS NULL
                OR u.uid = :uid
            )
            
            AND (
                :seasonId IS NULL OR gh.season_id = :seasonId
            )
            
            AND (
                :gameId IS NULL OR gh.game_id = :gameId
            )

    
        ORDER BY
            CASE WHEN :order = 'DESC' THEN gh.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN gh.created_at END ASC
    
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
        seasonId: Long?,
        gameId: Long?,
    ): Flow<AdminPlayResponse>




    @Query("""
        SELECT COUNT(*)
        FROM game_history gh
        LEFT JOIN season_participants sp ON sp.id = gh.participant_id 
        LEFT JOIN users u ON sp.user_id = u.id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        SELECT u.nickname
                        FROM users u
                        WHERE u.id = sp.user_id
                    ) ILIKE CONCAT('%', :search, '%')
                )
                OR (
                    :searchType = 'nickname'
                    AND (
                        SELECT u.nickname
                        FROM users u
                        WHERE u.id = sp.user_id
                    ) ILIKE CONCAT('%', :search, '%')
                )
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'PLAYING' AND gh.status = 'PLAYING')
                OR (:status = 'DONE' AND gh.status = 'DONE')
            )
    
            AND (
                :uid IS NULL
                OR u.uid = :uid
            )
            
            AND (
                :seasonId IS NULL OR gh.season_id = :seasonId
            )
            
            AND (
                :gameId IS NULL OR gh.game_id = :gameId
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        seasonId: Long?,
        gameId: Long?,
    ): Int

}
