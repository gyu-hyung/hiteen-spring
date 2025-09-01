package kr.jiasoft.hiteen.feature.chat.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatMessageEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ChatMessageRepository : CoroutineCrudRepository<ChatMessageEntity, Long> {
    suspend fun findByUid(uid: UUID): ChatMessageEntity?

    /** 채팅방 메시지 페이징 (keyset) */
    @Query("""
        SELECT * FROM chat_messages
        WHERE chat_room_id = :roomId
          AND (:cursor IS NULL OR created_at < :cursor)
        ORDER BY created_at DESC
        LIMIT :size
    """)
    fun pageByRoom(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<ChatMessageEntity>

    /** 방 최신 메세지 1건 조회 */
    @Query("""
        SELECT * FROM chat_messages
        WHERE chat_room_id = :roomId
        ORDER BY created_at DESC
        LIMIT 1
    """)
    suspend fun findLastMessage(roomId: Long): ChatMessageEntity?

    /** 특정 방에서 사용자 ID로 cursor 조회 */
    @Query("""
        SELECT COALESCE(MAX(m.id), 0)
        FROM chat_messages m
        JOIN chat_users cu
          ON cu.chat_room_id = m.chat_room_id
         AND cu.user_id      = :userId
         AND cu.deleted_at   IS NULL
        WHERE m.deleted_at IS NULL
    """)
    suspend fun currentCursorForUser(userId: Long): Long
}
