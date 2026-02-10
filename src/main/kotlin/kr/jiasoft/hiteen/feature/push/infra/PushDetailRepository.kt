package kr.jiasoft.hiteen.feature.push.infra

import kr.jiasoft.hiteen.feature.notification.dto.PushNotificationResponse
import kr.jiasoft.hiteen.feature.push.domain.PushDetailEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PushDetailRepository : CoroutineCrudRepository<PushDetailEntity, Long> {

    @Query("UPDATE push_detail SET deleted_at = NOW() WHERE user_id = :userId AND deleted_at IS NULL")
    suspend fun softDeleteByUserId(userId: Long)

    @Query("UPDATE push_detail SET deleted_at = NOW() WHERE id = :id AND user_id = :userId AND deleted_at IS NULL")
    suspend fun softDeleteByIdAndUserId(id: Long, userId: Long)


    @Query("""
        SELECT 
            d.id, 
            d.push_id, 
            p.code, 
            p.title, 
            p.message, 
            d.success,
            u.nickname AS nickname,
            u.asset_uid AS asset_uid,
            p.target_type,
            p.target_id,
            d.created_at 
        FROM push_detail d
        JOIN push p ON p.id = d.push_id
        LEFT JOIN users u ON u.id = p.created_id
        WHERE d.deleted_at IS NULL 
        AND d.user_id = :userId
        AND (:cursor IS NULL OR d.id < :cursor)
        AND (
            (:code IS NULL AND p.code <> 'CHAT_MESSAGE')
            OR p.code = :code
        )
        ORDER BY d.id DESC
        LIMIT :limit
    """)
    suspend fun findByUserIdWithCursor(
        userId: Long,
        cursor: Long?,
        limit: Int,
        code: String?
    ): List<PushNotificationResponse>

    @Query("""
        SELECT
            d.id,
            d.push_id,
            p.code,
            p.title,
            p.message,
            d.success,
            u.nickname AS nickname,
            u.asset_uid AS asset_uid,
            p.target_type,
            p.target_id,
            d.created_at
        FROM push_detail d
        JOIN push p ON p.id = d.push_id
        LEFT JOIN users u ON u.id = p.created_id
        WHERE d.deleted_at IS NULL
          AND d.user_id = :userId
          AND (:cursor IS NULL OR d.id < :cursor)
          AND (
            :codes IS NULL
            OR cardinality(:codes) = 0
            OR p.code = ANY(:codes)
          )
        ORDER BY d.id DESC
        LIMIT :limit
    """)
    suspend fun findByUserIdWithCursorAndCodes(
        userId: Long,
        cursor: Long?,
        limit: Int,
        codes: Array<String>?
    ): List<PushNotificationResponse>

}