package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminSmsRepository : CoroutineCrudRepository<SmsEntity, Long> {

    @Query(
        """
        SELECT COUNT(*)
        FROM sms s
        WHERE (
            :search IS NULL
            OR (
                :searchType = 'ALL'
                OR (:searchType = 'PHONE' AND COALESCE(s.callback, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'CONTENT' AND COALESCE(s.content, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'TITLE' AND COALESCE(s.title, '') ILIKE ('%' || :search || '%'))
            )
        )
        """
    )
    suspend fun countList(search: String?, searchType: String): Int

    @Query(
        """
        SELECT *
        FROM sms s
        WHERE (
            :search IS NULL
            OR (
                :searchType = 'ALL'
                OR (:searchType = 'PHONE' AND COALESCE(s.callback, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'CONTENT' AND COALESCE(s.content, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'TITLE' AND COALESCE(s.title, '') ILIKE ('%' || :search || '%'))
            )
        )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN s.id END ASC,
            CASE WHEN :order = 'DESC' THEN s.id END DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun list(search: String?, searchType: String, order: String, limit: Int, offset: Int): List<SmsEntity>
}

