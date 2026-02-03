package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatUserEntity
import kr.jiasoft.hiteen.feature.chat.dto.ActiveUsersRow
import kr.jiasoft.hiteen.feature.chat.dto.ChatUserNicknameProjection
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminChatUserRepository : CoroutineCrudRepository<ChatUserEntity, Long> {

    // 채팅방 참여자 목록 (나간 사람 제외)
    @Query("SELECT * FROM chat_users WHERE chat_room_id = :roomId AND deleted_at IS NULL")
    suspend fun listActiveByRoom(roomId: Long): Flow<ChatUserEntity>

    // 채팅방 참여자수 (나간 사람 제외)
    @Query("SELECT COUNT(*) FROM chat_users WHERE chat_room_id = :roomId AND deleted_at IS NULL")
    suspend fun countActiveByRoom(roomId: Long): Long

    // 채팅방 참여자 ID 목록 (나간 사람 제외)
    @Query("SELECT user_id FROM chat_users WHERE chat_room_id = :roomId AND deleted_at IS NULL")
    suspend fun listActiveUserIds(roomId: Long): Flow<Long>

    // 채팅방 참여자 UID 목록 (나간 사람 제외)
    @Query("""
        SELECT cu.user_id, u.uid AS user_uid
        FROM chat_users cu
        LEFT JOIN users u ON u.id = cu.user_id
        WHERE cu.chat_room_id = :roomId AND cu.deleted_at IS NULL
    """)
    suspend fun listActiveUserUids(roomId: Long): Flow<ActiveUsersRow>

    // 채팅방 참여자 UID 목록 (나간 사람 제외)
    @Query("""
        SELECT u.uid
        FROM chat_users cu
        LEFT JOIN users u ON u.id = cu.user_id
        LEFT JOIN chat_rooms cr on cr.id = cu.chat_room_id 
        WHERE cr.uid = :roomUid AND cu.deleted_at IS NULL
    """)
    suspend fun listActiveUserUidsByUid(roomUid: UUID): Flow<UUID>

    // 채팅방 참여자 정보 (나간 사람 제외)
    @Query("SELECT * FROM chat_users WHERE chat_room_id = :roomId AND user_id = :userId AND deleted_at IS NULL")
    suspend fun findActiveUser(roomId: Long, userId: Long): ChatUserEntity?

    // 채팅방 참여자 UID (나간 사람 제외)
    @Query("""
        SELECT u.uid
        FROM chat_users cu
        LEFT JOIN users u ON u.id = cu.user_id
        WHERE cu.chat_room_id = :roomId AND cu.user_id = :userId AND cu.deleted_at IS NULL
    """)
    suspend fun findUidOfActiveUser(roomId: Long, userId: Long): UUID?

    // 회원이 채팅방에 참여중인지 여부
    @Query("""
        SELECT EXISTS(
            SELECT 1 
            FROM chat_users cu
            JOIN chat_rooms cr ON cr.id = cu.chat_room_id
            WHERE cr.uid = :roomUid
              AND cu.user_id = :userId
              AND cu.deleted_at IS NULL
              AND cr.deleted_at IS NULL
        )
    """)
    suspend fun existsUserInRoom(roomUid: UUID, userId: Long): Boolean

    @Query("""
        SELECT cu.*, u.nickname
        FROM chat_users cu
        LEFT JOIN users u ON u.id = cu.user_id
        WHERE cu.chat_room_id IN (:roomIds) AND cu.deleted_at IS NULL
    """)
    suspend fun findAllDetailedByRoomIds(roomIds: List<Long>): Flow<ChatUserNicknameProjection>

}
