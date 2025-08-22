package kr.jiasoft.hiteen.feature.user.infra

import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<UserEntity, Long> {
    suspend fun findByUsername(name: String): UserEntity?
}