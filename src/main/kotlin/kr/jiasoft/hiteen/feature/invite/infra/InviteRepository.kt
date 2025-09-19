package kr.jiasoft.hiteen.feature.invite.infra

import kr.jiasoft.hiteen.feature.invite.domain.InviteEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InviteRepository : CoroutineCrudRepository<InviteEntity, Long> {
    suspend fun countByPhone(phone: String): Long
}
