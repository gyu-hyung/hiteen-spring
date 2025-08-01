package kr.jiasoft.hiteen.feature.user

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<UserEntity, Long> {
    suspend fun findByUsername(name: String): UserEntity?
}