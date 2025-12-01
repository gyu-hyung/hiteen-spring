package kr.jiasoft.hiteen.feature.push.infra

import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.notification.dto.PushNotificationResponse
import kr.jiasoft.hiteen.feature.push.domain.PushDetailEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PushDetailRepository : CoroutineCrudRepository<PushDetailEntity, Long> {

    suspend fun deleteByUserId(userId: Long)

    suspend fun deleteByPushIdAndUserId(pushId: Long, userId: Long)


    @Query("""
        SELECT d.id, d.push_id, p.code, p.title, p.message, d.success, d.created_at
        FROM push_detail d
        JOIN push p ON p.id = d.push_id
        WHERE d.deleted_at IS NULL 
        AND d.user_id = :userId
        AND (:cursor IS NULL OR d.id < :cursor)
        AND (:code IS NULL OR p.code = :code)        
        ORDER BY d.id DESC
        LIMIT :limit
    """)
    suspend fun findByUserIdWithCursor(
        userId: Long,
        cursor: Long?,
        limit: Int,
        code: String?
    ): List<PushNotificationResponse>



}