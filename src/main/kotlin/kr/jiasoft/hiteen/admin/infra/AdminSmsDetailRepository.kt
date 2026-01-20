package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsDetailEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminSmsDetailRepository : CoroutineCrudRepository<SmsDetailEntity, Long> {

    @Query(
        """
        SELECT *
        FROM sms_details d
        WHERE d.sms_id = :smsId
          AND (:includeDeleted = TRUE OR d.deleted_at IS NULL)
        ORDER BY d.created_at DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun listBySmsId(
        smsId: Long,
        includeDeleted: Boolean,
        limit: Int,
        offset: Int,
    ): List<SmsDetailEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM sms_details d
        WHERE d.sms_id = :smsId
          AND (:includeDeleted = TRUE OR d.deleted_at IS NULL)
        """
    )
    suspend fun countBySmsId(smsId: Long, includeDeleted: Boolean): Int
}

