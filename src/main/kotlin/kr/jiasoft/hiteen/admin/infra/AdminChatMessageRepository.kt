package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminChatMessageResponse
import kr.jiasoft.hiteen.feature.chat.domain.ChatMessageEntity
import kr.jiasoft.hiteen.feature.chat.dto.ReadersCountRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime
import java.util.UUID

interface AdminChatMessageRepository : CoroutineCrudRepository<ChatMessageEntity, Long> {
    suspend fun findByUid(uid: UUID): ChatMessageEntity?

    // 채팅 메시지 전체 개수
    @Query("""
        SELECT COUNT(*)
        FROM chat_messages cm
        LEFT JOIN chat_rooms cr ON cr.id = cm.chat_room_id
        LEFT JOIN users cu ON cu.id = cm.user_id
        WHERE
            (
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
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        CAST(cr.uid AS TEXT) ILIKE ('%' || :search || '%')
                        OR COALESCE(cr.room_name, '') ILIKE '%' || :search || '%'
                        OR COALESCE(cm.content, '') ILIKE '%' || :search || '%'
                        OR COALESCE(cu.nickname, '') ILIKE '%' || :search || '%'
                        OR COALESCE(cu.phone, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CODE' THEN
                        CAST(cr.uid AS TEXT) ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'ROOM' THEN
                        COALESCE(cr.room_name, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CONTENT' THEN
                        COALESCE(cm.content, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'NAME' THEN
                        COALESCE(cu.nickname, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'PHONE' THEN
                        COALESCE(cu.phone, '') ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
            )
    """)
    suspend fun countBySearch(
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
    ): Int

    // 채팅 메시지 목록
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
            cu.asset_uid AS user_asset,
            cr.id AS room_id,
            cr.uid AS room_uid,
            cr.room_name,
            (
                SELECT COUNT(*) 
                FROM chat_users 
                WHERE chat_room_id = cm.chat_room_id
                    AND deleted_at IS NULL
            ) AS user_count,
            (
                SELECT COUNT(*) 
                FROM chat_users
                WHERE chat_room_id = cm.chat_room_id
                    AND deleted_at IS NULL
                    AND last_read_message_id >= cm.id
                    AND user_id <> cm.user_id
            ) AS read_count
        FROM chat_messages cm
        LEFT JOIN chat_rooms cr ON cr.id = cm.chat_room_id
        LEFT JOIN users cu ON cu.id = cm.user_id
        WHERE
            (
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
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        CAST(cr.uid AS TEXT) ILIKE ('%' || :search || '%')
                        OR COALESCE(cr.room_name, '') ILIKE '%' || :search || '%'
                        OR COALESCE(cm.content, '') ILIKE '%' || :search || '%'
                        OR COALESCE(cu.nickname, '') ILIKE '%' || :search || '%'
                        OR COALESCE(cu.phone, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CODE' THEN
                        CAST(cr.uid AS TEXT) ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'ROOM' THEN
                        COALESCE(cr.room_name, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CONTENT' THEN
                        COALESCE(cm.content, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'NAME' THEN
                        COALESCE(cu.nickname, '') ILIKE '%' || :search || '%'
                    WHEN :searchType = 'PHONE' THEN
                        COALESCE(cu.phone, '') ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
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
    suspend fun countReadersInRange(roomId: Long, minId: Long, maxId: Long): Flow<ReadersCountRow>

    // 채팅방 > 메시지 갯수
    @Query("""
        SELECT COUNT(*)
        FROM chat_messages cm
        WHERE cm.chat_room_id = :roomId
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND cm.deleted_at IS NULL)
                OR (:status = 'DELETED' AND cm.deleted_at IS NOT NULL)
            )
            AND (
                :search IS NULL
                OR COALESCE(cm.content, '') ILIKE ('%' || :search || '%')
            )
    """)
    suspend fun countByRoom(
        roomId: Long,
        status: String?,
        search: String?,
    ): Int

    // 채팅방 > 메시지 목록
    @Query("""
        SELECT
            cm.id,
            cm.uid,
            cm.content,
            cm.kind,
            cm.emoji_code,
            cm.emoji_count,
            cm.created_at,
            cm.deleted_at,
            cm.user_id,
            cu.uid AS user_uid,
            cu.nickname AS user_name,
            cu.phone AS user_phone,
            cu.asset_uid AS user_asset,
            (
                SELECT COUNT(*) 
                FROM chat_users 
                WHERE chat_room_id = cm.chat_room_id
                    AND deleted_at IS NULL
            ) AS user_count,
            (
                SELECT COUNT(*) 
                FROM chat_users
                WHERE chat_room_id = cm.chat_room_id
                    AND deleted_at IS NULL
                    AND last_read_message_id >= cm.id
                    AND user_id <> cm.user_id
            ) AS read_count
        FROM chat_messages cm
        LEFT JOIN users cu ON cu.id = cm.user_id
        WHERE cm.chat_room_id = :roomId
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND cm.deleted_at IS NULL)
                OR (:status = 'DELETED' AND cm.deleted_at IS NOT NULL)
            )
            AND (
                :search IS NULL
                OR COALESCE(cm.content, '') ILIKE ('%' || :search || '%')
            )
        ORDER BY cm.created_at DESC, cm.id DESC
        LIMIT :perPage OFFSET :offset
    """)
    suspend fun listByRoom(
        roomId: Long,
        status: String?,
        search: String?,
        perPage: Int,
        offset: Int,
    ): Flow<AdminChatMessageResponse>
}
