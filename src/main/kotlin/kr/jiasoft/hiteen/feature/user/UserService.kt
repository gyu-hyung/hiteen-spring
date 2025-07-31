package kr.jiasoft.hiteen.feature.user

import org.springframework.stereotype.Service

@Service
class UserService (
  private val userRepository: UserRepository,
) {
    suspend fun getUserById(id: Long): UserEntity? =
        userRepository.findById(id)

    suspend fun createUser(user: UserEntity): UserEntity =
        userRepository.save(user)
}