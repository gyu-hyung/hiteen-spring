package kr.jiasoft.hiteen.feature.sms.infra

import kr.jiasoft.hiteen.feature.sms.domain.SmsDetailEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SmsDetailRepository : CoroutineCrudRepository<SmsDetailEntity, Long>

