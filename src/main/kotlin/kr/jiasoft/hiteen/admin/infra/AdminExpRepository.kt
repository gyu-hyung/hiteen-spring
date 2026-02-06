package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminExpResponse
import kr.jiasoft.hiteen.feature.level.domain.UserExpHistoryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AdminExpRepository : CoroutineCrudRepository<UserExpHistoryEntity, Long> {
    @Query("""
        SELECT COUNT(*)
        FROM user_exp_history e
        LEFT JOIN users u ON e.user_id = u.id
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'CREDIT' AND e.points > 0)
                OR (:status = 'DEBIT' AND e.points < 0)
            )
            AND (
                :type IS NULL OR :status = 'ALL'
                OR e.action_code LIKE CONCAT(:type, '%')
            )
            AND (
                :startDate IS NULL
                OR e.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR e.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    CASE
                        WHEN :searchType = 'ALL' THEN
                            u.nickname ILIKE '%' || :search || '%'
                            OR u.phone ILIKE '%' || :search || '%'
                            OR e.reason ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'NAME' THEN
                            u.nickname ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'PHONE' THEN
                            u.phone ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'MEMO' THEN
                            e.reason ILIKE '%' || :search || '%'
                    END
                )
            )
            AND (
                :uid IS NULL OR u.uid = :uid
            )
    """)
    suspend fun countSearch(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        uid: UUID?,
    ): Int

    @Query("""
        SELECT 
            e.*,
            u.uid AS user_uid,
            u.nickname AS user_name,
            u.phone AS user_phone
        FROM user_exp_history e
        LEFT JOIN users u ON e.user_id = u.id
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'CREDIT' AND e.points > 0)
                OR (:status = 'DEBIT' AND e.points < 0)
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR e.action_code LIKE CONCAT(:type, '%')
            )
            AND (
                :startDate IS NULL
                OR e.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR e.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    CASE
                        WHEN :searchType = 'ALL' THEN
                            u.nickname ILIKE '%' || :search || '%'
                            OR u.phone ILIKE '%' || :search || '%'
                            OR e.reason ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'NAME' THEN
                            u.nickname ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'PHONE' THEN
                            u.phone ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'MEMO' THEN
                            e.reason ILIKE '%' || :search || '%'
                    END
                )
            )
            AND (
                :uid IS NULL OR u.uid = :uid
            )
        ORDER BY id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun listSearch(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        uid: UUID?,
        sort: String?,
        limit: Int,
        offset: Int,
    ): Flow<AdminExpResponse>
}