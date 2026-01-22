package kr.jiasoft.hiteen.feature.chat.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatMessageEntity
import kr.jiasoft.hiteen.feature.chat.dto.ReadersCountRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ChatMessageRepository : CoroutineCrudRepository<ChatMessageEntity, Long> {
    suspend fun findByUid(uid: UUID): ChatMessageEntity?

    /** 채팅방 메시지 페이징 */
    @Query("""
        SELECT * FROM chat_messages
        WHERE chat_room_id = :roomId
          AND (:cursor IS NULL OR created_at < :cursor)
        ORDER BY created_at DESC, id DESC
        LIMIT :size
    """)
    fun pageByRoom(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<ChatMessageEntity>

    /** 방 최신 메세지 1건 조회 */
    @Query("""
        SELECT * FROM chat_messages
        WHERE chat_room_id = :roomId
        ORDER BY created_at DESC, id DESC
        LIMIT 1
    """)
    suspend fun findLastMessage(roomId: Long): ChatMessageEntity?

    /** 방 안읽은 메세지 수 */
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

    /** 메세지 목록 min ~ max 까지  message_id, reader_count 조회 */
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

    /** 특정 방에서 사용자 ID로 현재 cursor 조회 */
    @Query("""
        SELECT COALESCE(MAX(m.id), 0)
        FROM chat_messages m
        JOIN chat_users cu
          ON cu.chat_room_id = m.chat_room_id
         AND cu.user_id      = :userId
         AND cu.deleted_at   IS NULL
        WHERE m.deleted_at IS NULL
    """)
    suspend fun findCurrentCursorByUserId(userId: Long): Long

    /** 여러 방의 안읽은 메세지 수 일괄 조회 */
    @Query("""
        SELECT m.chat_room_id AS message_id, COUNT(*)::bigint AS reader_count
        FROM chat_messages m
        JOIN chat_users cu ON cu.chat_room_id = m.chat_room_id
        WHERE cu.user_id = :userId
          AND cu.chat_room_id IN (:roomIds)
          AND m.id > COALESCE(cu.last_read_message_id, 0)
          AND m.user_id <> :userId
          AND m.deleted_at IS NULL
          AND cu.deleted_at IS NULL
        GROUP BY m.chat_room_id
    """)
    fun countUnreadByRoomIds(roomIds: List<Long>, userId: Long): Flow<ReadersCountRow>

    /** 채팅방 메시지 페이징: 이모지 제외 */
    @Query("""
        SELECT * FROM chat_messages
        WHERE chat_room_id = :roomId
          AND deleted_at IS NULL
          AND kind <> 2
          AND (:cursor IS NULL OR created_at < :cursor)
        ORDER BY created_at DESC, id DESC
        LIMIT :size
    """)
    fun pageByRoomNoEmoji(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<ChatMessageEntity>

    /** 채팅방 메시지 페이징: 이모지 포함 */
    @Query("""
        SELECT * FROM chat_messages
        WHERE chat_room_id = :roomId
          AND deleted_at IS NULL
          AND (:cursor IS NULL OR created_at < :cursor)
        ORDER BY created_at DESC, id DESC
        LIMIT :size
    """)
    fun pageByRoomWithEmoji(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<ChatMessageEntity>

    /** 채팅방 메시지 페이징 요약 (최적화 버전) */
    @Query("""
        SELECT 
            m.id,
            m.uid as message_uid,
            m.user_id,
            m.content,
            m.kind,
            m.emoji_code,
            m.emoji_count,
            m.created_at,
            u.id as sender_id,
            u.uid as sender_uid,
            u.username as sender_username,
            u.nickname as sender_nickname,
            u.asset_uid::text as sender_asset_uid
        FROM chat_messages m
        JOIN users u ON u.id = m.user_id
        WHERE m.chat_room_id = :roomId
          AND m.deleted_at IS NULL
          AND (:cursor IS NULL OR m.created_at < :cursor)
        ORDER BY m.created_at DESC, m.id DESC
        LIMIT :size
    """)
    fun pageMessagesSummary(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<kr.jiasoft.hiteen.feature.chat.dto.MessageSummaryProjection>
}
