package kr.jiasoft.hiteen.feature.gift.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.gift.domain.GiftUsersEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GiftUserRepository: CoroutineCrudRepository<GiftUsersEntity, Long> {

    suspend fun findByUserId(userId: Long): Flow<GiftUsersEntity>
    suspend fun findByUserIdOrderByIdDesc(userId: Long): List<GiftUsersEntity>

}