package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface AdminSmsRepository : CoroutineCrudRepository<SmsEntity, Long> {

    @Query("""
        SELECT COUNT(*)
        FROM sms s
        WHERE
            (
                :startDate IS NULL
                OR s.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR s.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (
                            COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                            OR COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                            OR COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                            OR EXISTS (
                                SELECT 1
                                FROM sms_auth sa
                                WHERE sa.sms_id = s.id
                                    AND COALESCE(sa.phone, '') ILIKE ('%' || :search || '%')
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM sms_details sd
                                WHERE sd.sms_id = s.id
                                    AND COALESCE(sd.phone, '') ILIKE ('%' || :search || '%')
                            )
                        )
                    WHEN :searchType = 'CALLBACK' THEN
                        COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'TITLE' THEN
                        COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'MESSAGE' THEN
                        COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'PHONE' THEN
                        (
                            EXISTS (
                                SELECT 1
                                FROM sms_auth sa
                                WHERE sa.sms_id = s.id
                                    AND COALESCE(sa.phone, '') ILIKE ('%' || :search || '%')
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM sms_details sd
                                WHERE sd.sms_id = s.id
                                    AND COALESCE(sd.phone, '') ILIKE ('%' || :search || '%')
                            )
                        )
                    ELSE TRUE
                END
            )
    """)
    suspend fun countList(
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
    ): Int

    @Query("""
        SELECT *
        FROM sms s
        WHERE
            (
                :startDate IS NULL
                OR s.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR s.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (
                            COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                            OR COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                            OR COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                            OR EXISTS (
                                SELECT 1
                                FROM sms_auth sa
                                WHERE sa.sms_id = s.id
                                    AND COALESCE(sa.phone, '') ILIKE ('%' || :search || '%')
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM sms_details sd
                                WHERE sd.sms_id = s.id
                                    AND COALESCE(sd.phone, '') ILIKE ('%' || :search || '%')
                            )
                        )
                    WHEN :searchType = 'CALLBACK' THEN
                        COALESCE(s.callback, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'TITLE' THEN
                        COALESCE(s.title, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'MESSAGE' THEN
                        COALESCE(s.content, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'PHONE' THEN
                        (
                            EXISTS (
                                SELECT 1
                                FROM sms_auth sa
                                WHERE sa.sms_id = s.id
                                    AND COALESCE(sa.phone, '') ILIKE ('%' || :search || '%')
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM sms_details sd
                                WHERE sd.sms_id = s.id
                                    AND COALESCE(sd.phone, '') ILIKE ('%' || :search || '%')
                            )
                        )
                    ELSE TRUE
                END
            )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN s.id END ASC,
            CASE WHEN :order = 'DESC' THEN s.id END DESC
        LIMIT :limit OFFSET :offset
    """)
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

