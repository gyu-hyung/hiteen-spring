package kr.jiasoft.hiteen.feature.user.app

import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserReaderImpl(
    private val users: UserRepository
) : UserReader {
    override suspend fun findIdByUsername(username: String): Long? =
        users.findIdByUsername(username)

    override suspend fun findUidById(username: String): UUID? =
        users.findUidByUsername(username)

    override suspend fun findByUsername(username: String): UserEntity? =
        users.findByUsername(username)


}