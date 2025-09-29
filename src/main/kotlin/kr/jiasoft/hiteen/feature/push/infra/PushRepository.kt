package kr.jiasoft.hiteen.feature.push.infra

import kr.jiasoft.hiteen.feature.push.domain.PushEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PushRepository : CoroutineCrudRepository<PushEntity, Long> {
}