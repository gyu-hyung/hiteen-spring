package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface AdminSmsRepository : CoroutineCrudRepository<SmsEntity, Long> {

    @Query(
        """
        SELECT COUNT(*)
        FROM sms s
        WHERE (
                :startDate IS NULL
                OR s.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR s.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                    )
                )
                OR (
                    :searchType = 'PHONE'
                    AND COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'TITLE'
                     AND COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'MESSAGE'
                    AND COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                )
            )
        """
    )
    suspend fun countList(
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
    ): Int

    @Query(
        """
        SELECT *
        FROM sms s
        WHERE (
                :startDate IS NULL
                OR s.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR s.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                    )
                )
                OR (
                    :searchType = 'PHONE'
                    AND COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'TITLE'
                     AND COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'MESSAGE'
                    AND COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                )
            )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN s.id END ASC,
            CASE WHEN :order = 'DESC' THEN s.id END DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun list(
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        order: String,
        limit: Int,
        offset: Int,
    ): List<SmsEntity>
}

