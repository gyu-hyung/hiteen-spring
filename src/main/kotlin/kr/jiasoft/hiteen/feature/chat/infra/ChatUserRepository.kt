package kr.jiasoft.hiteen.feature.chat.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatUserEntity
import kr.jiasoft.hiteen.feature.chat.dto.ActiveUsersRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ChatUserRepository : CoroutineCrudRepository<ChatUserEntity, Long> {

    /** 방 멤버 목록 (나간사람 제외) */
    @Query("SELECT * FROM chat_users WHERE chat_room_id=:roomId AND deleted_at IS NULL")
    fun listByRoom(roomId: Long): Flow<ChatUserEntity>

    /** 방 활성 멤버 수 (나간사람 제외) */
    @Query("SELECT COUNT(*) FROM chat_users WHERE chat_room_id=:roomId AND deleted_at IS NULL")
    suspend fun countActiveByRoomId(roomId: Long): Long

    /** 방 활성 멤버 ID 목록 (나간사람 제외) */
    @Query("SELECT user_id FROM chat_users WHERE chat_room_id=:roomId AND deleted_at IS NULL")
    fun listActiveUserIds(roomId: Long): Flow<Long>

    /** 방 활성 멤버 UID 목록 (나간사람 제외) */
    @Query("SELECT (select uid from users u where u.id = cu.user_id) user_uid, cu.user_id FROM chat_users cu WHERE cu.chat_room_id = :roomId AND cu.deleted_at IS NULL")
    fun listActiveUserUids(roomId: Long): Flow<ActiveUsersRow>

    /** 방 활성 멤버 중 한 명 찾기 (나간사람 제외) */
    @Query("SELECT * FROM chat_users WHERE chat_room_id=:roomId AND user_id=:userId AND deleted_at IS NULL")
    suspend fun findActive(roomId: Long, userId: Long): ChatUserEntity?

    /** 방 활성 멤버 중 한 명 찾기 (나간사람 제외) */
    @Query("SELECT (select u.uid from users u where u.id = cu.user_id ) FROM chat_users cu WHERE cu.chat_room_id= :roomId  AND cu.user_id= :userId AND cu.deleted_at IS null")
    suspend fun findUidOfActiveUser(roomId: Long, userId: Long): UUID?

    /** 메세지 읽음 커서 업데이트 (단조 증가 앞으로만 전진) */
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
    """)
    suspend fun updateReadCursor(
        chatRoomId: Long,
        userId: Long,
        lastReadMessageId: Long,
        readAt: OffsetDateTime
    ): Int


    @Query("""
        SELECT EXISTS(
            SELECT 1 
            FROM chat_users cu
            JOIN chat_rooms r ON r.id = cu.chat_room_id
            WHERE r.uid = :roomUid
              AND cu.user_id = :userId
              AND cu.deleted_at IS NULL
              AND r.deleted_at IS NULL
        )
    """)
    suspend fun existsByRoomUidAndUserId(
        roomUid: UUID,
        userId: Long
    ): Boolean




}
