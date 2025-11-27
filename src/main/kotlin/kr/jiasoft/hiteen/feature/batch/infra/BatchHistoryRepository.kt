package kr.jiasoft.hiteen.feature.batch.infra

import kr.jiasoft.hiteen.feature.batch.domain.BatchHistoryEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BatchHistoryRepository : CoroutineCrudRepository<BatchHistoryEntity, Long>
