package kr.jiasoft.hiteen.feature.user.app

import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import java.util.UUID

interface UserReader {
    suspend fun findIdByUsername(username: String): Long?
    suspend fun findUidById(username: String): UUID?
    suspend fun findByUsername(username: String): UserEntity?
}