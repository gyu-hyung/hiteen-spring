package kr.jiasoft.hiteen.feature.gift.infra

import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface GiftRepository : CoroutineCrudRepository<GiftEntity, Long> {
    suspend fun findByUid(uid: UUID): GiftEntity?
}
