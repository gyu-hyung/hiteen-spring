package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.admin.dto.AdminChatRoomResponse
import kr.jiasoft.hiteen.feature.chat.domain.ChatRoomEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminChatRoomRepository : CoroutineCrudRepository<ChatRoomEntity, Long> {
    // 활성화된 채팅방수
    suspend fun countByDeletedAtIsNull() : Long
    // 삭제된 채팅방수
    suspend fun countByDeletedAtIsNotNull() : Long

    // 채팅방 정보
    @Query("""
        SELECT 
            cr.*,
            (
                SELECT COUNT(*) 
                FROM chat_users 
                WHERE chat_room_id = :roomId AND deleted_at IS NULL
            ) AS user_active_count,
            (
                SELECT COUNT(*) 
                FROM chat_users
                WHERE chat_room_id = :roomId AND deleted_at IS NOT NULL
            ) AS user_deleted_count
        FROM chat_rooms AS cr
        WHERE cr.id = :roomId
        LIMIT 1
    """)
    suspend fun detailById(roomId: Long): AdminChatRoomResponse
}
