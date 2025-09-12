package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollTemplateEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollTemplateRepository : CoroutineCrudRepository<PollTemplateEntity, Long> {
}