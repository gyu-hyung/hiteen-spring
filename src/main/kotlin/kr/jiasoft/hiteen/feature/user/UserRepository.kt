package kr.jiasoft.hiteen.feature.user

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<UserEntity, Long> {
//    suspend fun findByUid(id: String): UserEntity?
//    suspend fun findByPhone(phone: String): UserEntity?
}