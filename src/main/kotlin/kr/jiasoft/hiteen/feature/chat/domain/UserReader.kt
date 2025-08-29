package kr.jiasoft.hiteen.feature.chat.domain

import java.util.UUID

interface UserReader {
    suspend fun findIdByUsername(username: String): Long?
    suspend fun findUidById(username: String): UUID?
}
