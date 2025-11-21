package kr.jiasoft.hiteen.feature.chat.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatRoomEntity
import kr.jiasoft.hiteen.feature.chat.dto.ChatRoomResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface ChatRoomRepository : CoroutineCrudRepository<ChatRoomEntity, Long> {
    suspend fun findByUid(uid: UUID): ChatRoomEntity?
    suspend fun findByUidAndDeletedAtIsNull(uid: UUID): ChatRoomEntity?

    /** 두 유저로 구성된 1:1 방이 이미 있는지 검색 */
    @Query("""
        SELECT r.* FROM chat_rooms r
        JOIN chat_users u1 ON u1.chat_room_id = r.id AND u1.user_id = :userId1 AND u1.deleted_at IS NULL
        JOIN chat_users u2 ON u2.chat_room_id = r.id AND u2.user_id = :userId2 AND u2.deleted_at IS NULL
        WHERE r.deleted_at IS NULL
        and (select COUNT(*) from chat_users cu where cu.chat_room_id = r.id) = 2
    """)
    suspend fun findDirectRoom(userId1: Long, userId2: Long): ChatRoomEntity?


    /** 내가 속한 방 목록 (최근 메시지 시간순) */
    @Query("""
        SELECT 
            (select string_agg((select nickname from users where id = user_id), ',') from chat_users where chat_room_id = r.id and user_id != :userId) room_title,
			r.* 
        FROM chat_rooms r
        JOIN chat_users cu ON cu.chat_room_id = r.id
        WHERE cu.user_id = :userId
        AND r.last_message_id IS NOT NULL
        AND cu.deleted_at IS NULL 
        AND r.deleted_at IS NULL
        ORDER BY COALESCE(r.updated_at, r.created_at) DESC NULLS LAST
        LIMIT :limit OFFSET :offset
    """)
    fun listRooms(userId: Long, limit: Int, offset: Int): Flow<ChatRoomResponse>

    /** 내가 속한 방 목록 (최근 메시지 시간순) */
    @Query("""
        SELECT
			r.uid 
        FROM chat_rooms r
        JOIN chat_users cu ON cu.chat_room_id = r.id
        WHERE cu.user_id = :userId
        AND r.last_message_id IS NOT NULL
        AND cu.deleted_at IS NULL 
        AND r.deleted_at IS NULL
    """)
    suspend fun listRoomUids(userId: Long): Flow<UUID>?


    /** 동일 멤버셋(정확히 일치)인 방 한 개 찾기 (활성 멤버 기준) */
    @Query("""
        SELECT r.* 
        FROM chat_rooms r
        JOIN chat_users cu ON cu.chat_room_id = r.id AND cu.deleted_at IS NULL
        WHERE r.deleted_at IS NULL
        GROUP BY r.id
        HAVING COUNT(*) = :size
           AND COUNT(*) FILTER (WHERE cu.user_id IN (:memberIds)) = :size
        LIMIT 1
    """)
    suspend fun findRoomByExactActiveMembers(memberIds: List<Long>, size: Int): ChatRoomEntity?
}

