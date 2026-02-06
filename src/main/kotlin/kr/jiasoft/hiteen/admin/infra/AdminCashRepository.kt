package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminCashResponse
import kr.jiasoft.hiteen.feature.cash.domain.CashEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AdminCashRepository : CoroutineCrudRepository<CashEntity, Long> {
    @Query("""
        SELECT COUNT(*)
        FROM cash c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.deleted_at IS NULL
            AND (
                :type IS NULL OR :type = 'ALL'
                OR c.cashable_type LIKE CONCAT(:type, '%')
            )
            AND (
                :startDate IS NULL
                OR c.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR c.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        u.nickname ILIKE '%' || :search || '%'
                        OR u.phone ILIKE '%' || :search || '%'
                        OR c.memo ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'phone' THEN
                        u.phone ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'memo' THEN
                        c.memo ILIKE '%' || :search || '%'
            
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
            c.*,
            u.uid AS user_uid,
            u.nickname AS nickname,
            u.phone AS phone
        FROM cash c
        LEFT JOIN users u ON c.user_id = u.id
        WHERE c.deleted_at IS NULL
            AND (
                :type IS NULL
                OR c.cashable_type LIKE CONCAT(:type, '%')
            )
            AND (
                :startDate IS NULL
                OR c.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR c.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        u.nickname ILIKE '%' || :search || '%'
                        OR u.phone ILIKE '%' || :search || '%'
                        OR c.memo ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'phone' THEN
                        u.phone ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'memo' THEN
                        c.memo ILIKE '%' || :search || '%'
            
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
    fun listSearchResults(
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        uid: UUID?,
        limit: Int,
        offset: Int,
    ): Flow<AdminCashResponse>
}

