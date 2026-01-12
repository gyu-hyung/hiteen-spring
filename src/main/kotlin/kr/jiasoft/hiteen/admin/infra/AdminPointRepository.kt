package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminPointResponse
import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AdminPointRepository : CoroutineCrudRepository<PointEntity, Long> {
    @Query("""
        SELECT COUNT(*)
        FROM points p
        LEFT JOIN users u ON p.user_id = u.id
        WHERE p.deleted_at IS NULL
            AND (
                :type IS NULL
                OR p.pointable_type LIKE CONCAT(:type, '%')
            )
            AND (
                :startDate IS NULL
                OR p.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR p.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        u.nickname LIKE CONCAT('%', :search, '%')
                        OR u.phone LIKE CONCAT('%', :search, '%')
                        OR p.memo LIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'nickname' AND u.nickname LIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'phone' AND u.phone LIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'memo' AND p.memo LIKE CONCAT('%', :search, '%'))
            )
            AND (
                :uid IS NULL
                OR u.uid = :uid
            )
    """)
    suspend fun countSearchResults(
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        uid: UUID?,
    ): Int

    @Query("""
        SELECT 
            p.*,
            u.uid AS user_uid,
            u.nickname AS nickname,
            u.phone AS phone
        FROM points p
        LEFT JOIN users u ON p.user_id = u.id
        WHERE p.deleted_at IS NULL
            AND (
                :type IS NULL
                OR p.pointable_type LIKE CONCAT(:type, '%')
            )
            AND (
                :startDate IS NULL
                OR p.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR p.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        u.nickname LIKE CONCAT('%', :search, '%')
                        OR u.phone LIKE CONCAT('%', :search, '%')
                        OR p.memo LIKE CONCAT('%', :search, '%')
                    )
                    OR (:searchType = 'nickname' AND u.nickname LIKE CONCAT('%', :search, '%'))
                    OR (:searchType = 'phone' AND u.phone LIKE CONCAT('%', :search, '%'))
                    OR (:searchType = 'memo' AND p.memo LIKE CONCAT('%', :search, '%'))
                )
            )
            AND (
                :uid IS NULL
                OR u.uid = :uid
            )
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun listSearchResults(
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        uid: UUID?,
        limit: Int,
        offset: Int,
    ): Flow<AdminPointResponse>
}