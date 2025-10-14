package kr.jiasoft.hiteen.feature.push.infra

import kr.jiasoft.hiteen.feature.push.domain.PushEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PushRepository : CoroutineCrudRepository<PushEntity, Long> {
}