package kr.jiasoft.hiteen.feature.sms.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SmsRepository : CoroutineCrudRepository<SmsEntity, Long> {
}