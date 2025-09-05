package kr.jiasoft.hiteen.feature.sms.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsAuthEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SmsAuthRepository : CoroutineCrudRepository<SmsAuthEntity, Long> {

    @Query("""
        SELECT * FROM sms_auth 
        WHERE phone = :phone
          AND status = 'WAITING'
          AND created_at > NOW() - (:minutes * INTERVAL '1 minute')
        ORDER BY created_at DESC
        LIMIT 1
    """)
    suspend fun findValidAuthCode(phone: String, minutes: Int = 5): SmsAuthEntity?

}