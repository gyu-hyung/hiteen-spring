package kr.jiasoft.hiteen.feature.ad.infra

import kr.jiasoft.hiteen.feature.ad.domain.AdmobRewardEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdmobRewardRepository : CoroutineCrudRepository<AdmobRewardEntity, Long> {
    suspend fun existsByTransactionId(transactionId: String): Boolean
}
