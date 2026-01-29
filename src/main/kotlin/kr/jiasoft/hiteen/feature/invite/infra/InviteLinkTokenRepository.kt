package kr.jiasoft.hiteen.feature.invite.infra

import kr.jiasoft.hiteen.feature.invite.domain.InviteLinkTokenEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InviteLinkTokenRepository : CoroutineCrudRepository<InviteLinkTokenEntity, Long> {
    suspend fun findByToken(token: String): InviteLinkTokenEntity?
}

