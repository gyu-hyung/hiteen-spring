package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminChatMessageResponse
import kr.jiasoft.hiteen.feature.chat.domain.ChatMessageEntity
import kr.jiasoft.hiteen.feature.chat.dto.ReadersCountRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

interface AdminChatMessageRepository : CoroutineCrudRepository<ChatMessageEntity, Long> {
    suspend fun findByUid(uid: UUID): ChatMessageEntity?

    // 전체 개수
    @Query("""
        SELECT COUNT(*)
        FROM chat_messages cm
        LEFT JOIN chat_rooms cr ON cr.id = cm.chat_room_id
        LEFT JOIN users cu ON cu.id = cm.user_id
        WHERE (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND cm.deleted_at IS NULL)
                OR (:status = 'DELETED' AND cm.deleted_at IS NOT NULL)
            )
            AND (
                :startDate IS NULL
                OR cm.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR cm.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        COALESCE(cr.room_name, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(cm.content, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(cu.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(cu.phone, '') ILIKE ('%' || :search || '%')
                    )
                )
                OR (
                    :searchType = 'ROOM'
                     AND COALESCE(cr.room_name, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'CONTENT'
                     AND COALESCE(cm.content, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'NAME'
                     AND COALESCE(cu.nickname, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'PHONE'
                    AND COALESCE(cu.phone, '') ILIKE ('%' || :search || '%')
                )
            )
    """)
    suspend fun countBySearch(
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
    ): Int

    // 페이징 조회
    @Query("""
        SELECT 
            cm.id,
            cm.uid,
            cm.user_id,
            cm.content,
            cm.kind,
            cm.emoji_code,
            cm.emoji_count,
            cm.created_at,
            cm.deleted_at,
            cu.uid AS user_uid,
            cu.nickname AS user_name,
            cu.phone AS user_phone,
            cr.id AS room_id,
            cr.uid AS room_uid,
            cr.room_name,
            (SELECT COUNT(*) FROM chat_users WHERE chat_room_id = cm.chat_room_id AND deleted_at IS NULL) AS user_count
        FROM chat_messages cm
        LEFT JOIN chat_rooms cr ON cr.id = cm.chat_room_id
        LEFT JOIN users cu ON cu.id = cm.user_id
        WHERE (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND cm.deleted_at IS NULL)
                OR (:status = 'DELETED' AND cm.deleted_at IS NOT NULL)
            )
            AND (
                :startDate IS NULL
                OR cm.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR cm.created_at < :endDate
            )
            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        COALESCE(cr.room_name, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(cm.content, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(cu.nickname, '') ILIKE ('%' || :search || '%')
                        OR COALESCE(cu.phone, '') ILIKE ('%' || :search || '%')
                    )
                )
                OR (
                    :searchType = 'ROOM'
                     AND COALESCE(cr.room_name, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'CONTENT'
                     AND COALESCE(cm.content, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'NAME'
                     AND COALESCE(cu.nickname, '') ILIKE ('%' || :search || '%')
                )
                OR (
                    :searchType = 'PHONE'
                    AND COALESCE(cu.phone, '') ILIKE ('%' || :search || '%')
                )
            )
        ORDER BY cm.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun listBySearch(
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        limit: Int,
        offset: Int,
    ): Flow<AdminChatMessageResponse>

    // 채팅방 메시지 목록: 삭제된 메시지 포함
    @Query("""
        SELECT * FROM chat_messages
        WHERE chat_room_id = :roomId
            AND (:cursor IS NULL OR created_at < :cursor)
        ORDER BY created_at DESC, id DESC
        LIMIT :size
    """)
    suspend fun listByRoom(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<AdminChatMessageResponse>

    // 채팅 메시지 읽음수 목록
    @Query("""
        SELECT
            cm.id AS message_id,
            SUM(CASE WHEN cu.last_read_message_id >= cm.id AND cu.user_id <> cm.user_id THEN 1 ELSE 0 END) AS reader_count
        FROM chat_messages cm
        JOIN chat_users cu ON cu.chat_room_id = cm.chat_room_id AND cu.deleted_at IS NULL
        WHERE cm.chat_room_id = :roomId
            AND cm.id BETWEEN :minId AND :maxId
            AND cm.deleted_at IS NULL
        GROUP BY cm.id
    """)
    suspend fun countReadersInIdRange(roomId: Long, minId: Long, maxId: Long): Flow<ReadersCountRow>

    // 특정 채팅방에서 참여자 ID로 현재 cursor 조회
    @Query("""
        SELECT COALESCE(MAX(cm.id), 0)
        FROM chat_messages cm
        JOIN chat_users cu ON cu.chat_room_id = cm.chat_room_id AND cu.user_id = :userId AND cu.deleted_at IS NULL
        WHERE cm.deleted_at IS NULL
    """)
    suspend fun findCurrentCursorByUserId(userId: Long): Long

    // 채팅방 메시지 목록: 이모지 제외
    @Query("""
        SELECT *
        FROM chat_messages
        WHERE chat_room_id = :roomId
          AND deleted_at IS NULL
          AND kind <> 2
          AND (:cursor IS NULL OR created_at < :cursor)
        ORDER BY created_at DESC, id DESC
        LIMIT :size
    """)
    suspend fun listByRoomWithoutEmoji(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<AdminChatMessageResponse>

    // 채팅방 메시지 목록: 이모지 포함
    @Query("""
        SELECT *
        FROM chat_messages
        WHERE chat_room_id = :roomId
          AND deleted_at IS NULL
          AND (:cursor IS NULL OR created_at < :cursor)
        ORDER BY created_at DESC, id DESC
        LIMIT :size
    """)
    suspend fun listByRoomWithEmoji(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<AdminChatMessageResponse>

    // 채팅방 메시지 목록 (최적화 버전)
    @Query("""
        SELECT 
            cm.id,
            cm.uid as message_uid,
            cm.user_id,
            cm.content,
            cm.kind,
            cm.emoji_code,
            cm.emoji_count,
            cm.created_at,
            cu.id as sender_id,
            cu.uid as sender_uid,
            cu.username as sender_username,
            cu.nickname as sender_nickname,
            cu.asset_uid::text AS sender_asset_uid
        FROM chat_messages cm
        JOIN users cu ON cu.id = cm.user_id
        WHERE cm.chat_room_id = :roomId
            AND cm.deleted_at IS NULL
            AND (:cursor IS NULL OR cm.created_at < :cursor)
        ORDER BY cm.created_at DESC, cm.id DESC
        LIMIT :size
    """)
    suspend fun listByRoomSummary(roomId: Long, cursor: OffsetDateTime?, size: Int): Flow<AdminChatMessageResponse>
}
