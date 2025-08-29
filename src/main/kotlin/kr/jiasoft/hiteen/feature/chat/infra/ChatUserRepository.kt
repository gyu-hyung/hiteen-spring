package kr.jiasoft.hiteen.feature.chat.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatUserEntity
import kr.jiasoft.hiteen.feature.chat.dto.ReadersCountRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.OffsetDateTime

interface ChatUserRepository : CoroutineCrudRepository<ChatUserEntity, Long> {
    @Query("SELECT * FROM chat_users WHERE chat_room_id=:roomId AND deleted_at IS NULL")
    fun listByRoom(roomId: Long): Flow<ChatUserEntity>

    @Query("SELECT COUNT(*) FROM chat_users WHERE chat_room_id=:roomId AND deleted_at IS NULL")
    suspend fun countActiveByRoom(roomId: Long): Long

    @Query("SELECT * FROM chat_users WHERE chat_room_id=:roomId AND user_id=:userId AND deleted_at IS NULL")
    suspend fun findActive(roomId: Long, userId: Long): ChatUserEntity?

    /** 단조 증가(앞으로만 전진) */
    @Query("""
        UPDATE chat_users
        SET 
          last_read_message_id = CASE
            WHEN last_read_message_id IS NULL OR last_read_message_id < :lastReadMessageId
            THEN :lastReadMessageId
            ELSE last_read_message_id
          END,
          last_read_at = CASE
            WHEN :readAt IS NOT NULL AND (last_read_at IS NULL OR last_read_at < :readAt)
            THEN :readAt
            ELSE last_read_at
          END
        WHERE chat_room_id = :chatRoomId
          AND user_id      = :userId
          AND deleted_at IS NULL
    """
    )
    suspend fun updateReadCursor(
        chatRoomId: Long,
        userId: Long,
        lastReadMessageId: Long,
        readAt: OffsetDateTime
    ): Int


    @Query("""
        SELECT user_id FROM chat_users
        WHERE chat_room_id=:roomId AND deleted_at IS NULL
    """)
    fun listActiveUserIds(roomId: Long): Flow<Long>

    @Query("""
        SELECT COUNT(*)
        FROM chat_messages m
        WHERE m.chat_room_id = :roomId
          AND m.deleted_at IS NULL
          AND m.id > COALESCE((
                SELECT last_read_message_id
                FROM chat_users
                WHERE chat_room_id=:roomId AND user_id=:userId AND deleted_at IS NULL
              ), 0)
          AND m.user_id <> :userId
    """)
    suspend fun countUnread(roomId: Long, userId: Long): Long




    @Query("""
        SELECT m.id AS message_id,
               SUM(CASE WHEN cu.last_read_message_id >= m.id AND cu.user_id <> m.user_id THEN 1 ELSE 0 END) AS reader_count
          FROM chat_messages m
          JOIN chat_users cu
            ON cu.chat_room_id = m.chat_room_id
           AND cu.deleted_at   IS NULL
         WHERE m.chat_room_id = :roomId
           AND m.id BETWEEN :minId AND :maxId
           AND m.deleted_at IS NULL
        GROUP BY m.id
    """)
    fun countReadersInIdRange(
        roomId: Long,
        minId: Long,
        maxId: Long
    ): Flow<ReadersCountRow>

}
