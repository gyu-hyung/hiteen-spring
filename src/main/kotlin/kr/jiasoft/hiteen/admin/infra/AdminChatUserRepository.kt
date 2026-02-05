package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminChatUserResponse
import kr.jiasoft.hiteen.feature.chat.domain.ChatUserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminChatUserRepository : CoroutineCrudRepository<ChatUserEntity, Long> {
    // 채팅방 참여자 목록
    @Query("""
        SELECT 
            cu.*,
            u.uid AS user_uid,
            u.nickname AS user_name,
            u.phone AS user_phone,
            u.asset_uid AS asset_uid,
            CASE WHEN cu.user_id = cr.created_id THEN 'Y' ELSE 'N' END AS is_owner,
            CASE WHEN cu.leaving_at IS NOT NULL OR cu.deleted_at IS NOT NULL THEN 'Y' ELSE 'N' END AS is_leaved,
            CASE WHEN u.deleted_at IS NOT NULL THEN 'Y' ELSE 'N' END AS user_deleted
        FROM chat_users cu
        JOIN chat_rooms cr ON cr.id = cu.chat_room_id
        JOIN users u ON u.id = cu.user_id
        WHERE cr.id = :roomId
        ORDER BY cu.id ASC
    """)
    suspend fun usersById(roomId: Long): Flow<AdminChatUserResponse>

    // 회원이 채팅방에 참여중인지 여부
    @Query("""
        SELECT EXISTS(
            SELECT 1 
            FROM chat_users cu
            JOIN chat_rooms cr ON cr.id = cu.chat_room_id
            WHERE cr.id = :roomId
              AND cu.user_id = :userId
              AND cu.deleted_at IS NULL
              AND cr.deleted_at IS NULL
        )
    """)
    suspend fun existsUserInRoom(roomId: Long, userId: Long): Boolean
}
