package kr.jiasoft.hiteen.feature.user.app

import java.util.UUID

interface UserReader {
    suspend fun findIdByUsername(username: String): Long?
    suspend fun findUidById(username: String): UUID?
}