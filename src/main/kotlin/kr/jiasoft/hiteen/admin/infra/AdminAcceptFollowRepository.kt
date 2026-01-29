package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminAcceptFollowResponse
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface AdminAcceptFollowRepository : CoroutineCrudRepository<FollowEntity, Long> {

    // 전체 개수 조회
    @Query("""
        SELECT COUNT(*)
        FROM follows f
        JOIN users req ON f.user_id = req.id
        JOIN users res ON f.follow_id = res.id
        WHERE f.deleted_at IS NULL
            AND (
                :status IS NULL OR :status = 'ALL'
                OR f.status = :status
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
                    :searchType = 'ALL' AND (
                        COALESCE(req.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(req.phone, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(res.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(res.phone, '') ILIKE ('%' || :search || '%')
                    )
                )
                OR (
                    :searchType = 'SENDER'
                     AND (
                        COALESCE(req.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(req.phone, '') ILIKE ('%' || :search || '%')
                     )
                )
                OR (
                    :searchType = 'RECEIVER'
                    AND (
                        COALESCE(res.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(res.phone, '') ILIKE ('%' || :search || '%')
                    )
                )
            )
    """)
    suspend fun countBySearch(
        status: String?,
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
            f.follow_id AS target_id,
            res.nickname AS target_name,
            res.phone AS target_phone,

            f.status,
            f.status_at,
            f.created_at,
            f.updated_at,
            f.deleted_at
        FROM follows f
        JOIN users req ON f.user_id = req.id
        JOIN users res ON f.follow_id = res.id
        WHERE f.deleted_at IS NULL
            AND (
                :status IS NULL OR :status = 'ALL'
                OR f.status = :status
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
                    :searchType = 'ALL' AND (
                        COALESCE(req.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(req.phone, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(res.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(res.phone, '') ILIKE ('%' || :search || '%')
                    )
                )
                OR (
                    :searchType = 'SENDER'
                     AND (
                        COALESCE(req.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(req.phone, '') ILIKE ('%' || :search || '%')
                     )
                )
                OR (
                    :searchType = 'RECEIVER'
                    AND (
                        COALESCE(res.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(res.phone, '') ILIKE ('%' || :search || '%')
                     )
                )
            )
        ORDER BY
            CASE WHEN :sort = 'ASC' THEN f.id END ASC,
            CASE WHEN :sort <> 'ASC' THEN f.id END DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun listBySearch(
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        sort: String?,
        limit: Int,
        offset: Int,
    ): Flow<AdminAcceptFollowResponse>

}
