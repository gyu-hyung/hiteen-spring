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
                :type IS NULL OR :type = 'ALL'
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
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        u.nickname ILIKE '%' || :search || '%'
                        OR u.phone ILIKE '%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%'
                        OR p.memo ILIKE '%' || :search || '%'
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
                    WHEN :searchType = 'phone' THEN
                        u.phone ILIKE '%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%'
                    WHEN :searchType = 'memo' THEN
                        p.memo ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
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
                :type IS NULL OR :type = 'ALL'
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
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        u.nickname ILIKE '%' || :search || '%'
                        OR u.phone ILIKE '%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%'
                        OR p.memo ILIKE '%' || :search || '%'
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
                    WHEN :searchType = 'phone' THEN
                        u.phone ILIKE '%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%'
                    WHEN :searchType = 'memo' THEN
                        p.memo ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
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