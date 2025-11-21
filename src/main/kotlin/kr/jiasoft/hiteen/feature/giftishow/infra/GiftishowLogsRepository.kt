package kr.jiasoft.hiteen.feature.giftishow.infra

import kr.jiasoft.hiteen.feature.giftishow.domain.GiftishowLogsEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GiftishowLogsRepository : CoroutineCrudRepository<GiftishowLogsEntity, Long>
