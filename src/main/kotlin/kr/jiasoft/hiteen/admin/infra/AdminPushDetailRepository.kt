package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.push.domain.PushDetailEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminPushDetailRepository : CoroutineCrudRepository<PushDetailEntity, Long> {

    @Query("SELECT COUNT(*) FROM push_detail d WHERE d.push_id = :pushId AND d.deleted_at IS NULL")
    suspend fun countByPushId(pushId: Long): Int

    @Query(
        """
        SELECT *
        FROM push_detail d
        WHERE d.push_id = :pushId
          AND d.deleted_at IS NULL
          AND (
            :success = 'ALL'
            OR (:success = 'SUCCESS' AND d.success = 1)
            OR (:success = 'FAIL' AND d.success = 0)
          )
        ORDER BY d.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun listByPushId(pushId: Long, success: String, limit: Int, offset: Int): List<PushDetailEntity>
}

