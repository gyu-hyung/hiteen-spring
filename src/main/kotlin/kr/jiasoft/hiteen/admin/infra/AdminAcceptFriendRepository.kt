package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminAcceptFriendResponse
import kr.jiasoft.hiteen.feature.relationship.domain.FriendEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface AdminAcceptFriendRepository : CoroutineCrudRepository<FriendEntity, Long> {

    // 전체 개수 조회
    @Query("""
        SELECT COUNT(*)
        FROM friends f
        JOIN users req ON f.user_id = req.id
        JOIN users res ON f.friend_id = res.id
        WHERE f.deleted_at IS NULL
            AND (
                :status IS NULL OR :status = 'ALL'
                OR f.status = :status
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR f.user_location_mode = :type OR f.friend_location_mode = :type
            )
            AND (
                :startDate IS NULL
                OR f.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR f.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    CASE
                        WHEN :searchType = 'ALL' THEN
                            COALESCE(req.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(req.phone, '') ILIKE '%' || :search || '%'
                            OR COALESCE(res.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(res.phone, '') ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'SENDER' THEN
                            COALESCE(req.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(req.phone, '') ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'RECEIVER' THEN
                            COALESCE(res.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(res.phone, '') ILIKE '%' || :search || '%'
                    END
                )
            )
    """)
    suspend fun countBySearch(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
    ): Int


    // 페이징 조회
    @Query("""
        SELECT 
            f.id,

            -- 요청회원
            f.user_id,
            req.nickname AS user_name,
            req.phone AS user_phone,

            -- 수신회원
            f.friend_id AS target_id,
            res.nickname AS target_name,
            res.phone AS target_phone,

            f.status,
            f.status_at,
            f.user_location_mode,
            f.friend_location_mode,
            f.created_at,
            f.updated_at,
            f.deleted_at
        FROM friends f
        JOIN users req ON f.user_id = req.id
        JOIN users res ON f.friend_id = res.id
        WHERE f.deleted_at IS NULL
            AND (
                :status IS NULL OR :status = 'ALL'
                OR f.status = :status
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR f.user_location_mode = :type OR f.friend_location_mode = :type
            )
            AND (
                :startDate IS NULL
                OR f.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR f.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    CASE
                        WHEN :searchType = 'ALL' THEN
                            COALESCE(req.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(req.phone, '') ILIKE '%' || :search || '%'
                            OR COALESCE(res.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(res.phone, '') ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'SENDER' THEN
                            COALESCE(req.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(req.phone, '') ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'RECEIVER' THEN
                            COALESCE(res.nickname, '') ILIKE '%' || :search || '%'
                            OR COALESCE(res.phone, '') ILIKE '%' || :search || '%'
                    END
                )
            )
        ORDER BY
            CASE WHEN :sort = 'ASC' THEN f.id END ASC,
            CASE WHEN :sort <> 'ASC' THEN f.id END DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun listBySearch(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        sort: String?,
        limit: Int,
        offset: Int,
    ): Flow<AdminAcceptFriendResponse>

}
