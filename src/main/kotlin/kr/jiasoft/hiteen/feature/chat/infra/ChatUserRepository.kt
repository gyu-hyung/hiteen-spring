package kr.jiasoft.hiteen.feature.chat.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatUserEntity
import kr.jiasoft.hiteen.feature.chat.dto.ActiveUsersRow
import kr.jiasoft.hiteen.feature.chat.dto.ChatUserNicknameProjection
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

    /** 방 활성 멤버 UID 목록 (나간사람 제외) */
    @Query("""
        SELECT (select uid from users u where u.id = cu.user_id) user_uid
        FROM chat_users cu 
        left join chat_rooms cr on cr.id = cu.chat_room_id 
        WHERE cr.uid = :roomUid AND cu.deleted_at IS null
    """)
    fun listActiveUserUidsByUid(roomUid: UUID): Flow<UUID>

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

    /**
     * 초대/재입장 처리: (chat_room_id, user_id) 유니크를 활용해 upsert.
     * - 신규면 insert
     * - 기존 row가 있으면 deleted_at/leaving_at을 NULL로 되돌리고 joining_at 갱신
     */
    @Query("""
        INSERT INTO chat_users (chat_room_id, user_id, push, push_at, joining_at, leaving_at, deleted_at)
        VALUES (:chatRoomId, :userId, true, :pushAt, :joiningAt, NULL, NULL)
        ON CONFLICT (chat_room_id, user_id)
        DO UPDATE SET
            push = EXCLUDED.push,
            push_at = EXCLUDED.push_at,
            joining_at = EXCLUDED.joining_at,
            leaving_at = NULL,
            deleted_at = NULL
    """)
    suspend fun upsertRejoin(
        chatRoomId: Long,
        userId: Long,
        joiningAt: OffsetDateTime,
        pushAt: OffsetDateTime,
    ): Int

    @Query("""
        SELECT cu.*, u.nickname as nickname
        FROM chat_users cu
        JOIN users u ON u.id = cu.user_id
        WHERE cu.chat_room_id IN (:roomIds) AND cu.deleted_at IS NULL
    """)
    fun findAllDetailedByRoomIds(roomIds: List<Long>): Flow<ChatUserNicknameProjection>

}
