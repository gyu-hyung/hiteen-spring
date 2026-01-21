package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsAuthEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminSmsAuthRepository : CoroutineCrudRepository<SmsAuthEntity, Long> {

    @Query(
        """
        SELECT *
        FROM sms_auth a
        WHERE a.sms_id = :smsId
          AND (:includeDeleted = TRUE OR a.deleted_at IS NULL)
          AND (
            :status = 'ALL'
            OR a.status = :status
          )
        ORDER BY a.created_at DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun listBySmsId(
        smsId: Long,
        includeDeleted: Boolean,
        status: String,
        limit: Int,
        offset: Int,
    ): List<SmsAuthEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM sms_auth a
        WHERE a.sms_id = :smsId
          AND (:includeDeleted = TRUE OR a.deleted_at IS NULL)
          AND (
            :status = 'ALL'
            OR a.status = :status
          )
        """
    )
    suspend fun countBySmsId(smsId: Long, includeDeleted: Boolean, status: String): Int
}

