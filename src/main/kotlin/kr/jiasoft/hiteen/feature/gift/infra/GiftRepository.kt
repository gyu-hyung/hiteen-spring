package kr.jiasoft.hiteen.feature.gift.infra

import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GiftRepository : CoroutineCrudRepository<GiftEntity, Long>
