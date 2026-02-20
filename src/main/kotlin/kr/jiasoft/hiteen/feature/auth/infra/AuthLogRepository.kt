package kr.jiasoft.hiteen.feature.auth.infra

import kr.jiasoft.hiteen.feature.auth.domain.AuthLogEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthLogRepository : CoroutineCrudRepository<AuthLogEntity, Long>
