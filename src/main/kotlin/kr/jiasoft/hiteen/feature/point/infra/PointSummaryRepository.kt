package kr.jiasoft.hiteen.feature.point.infra

import kr.jiasoft.hiteen.feature.point.domain.PointSummaryEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PointSummaryRepository : CoroutineCrudRepository<PointSummaryEntity, Long> {

    @Query("""
        INSERT INTO user_points_summary (user_id, total_point, updated_at)
        VALUES (:userId, :delta, now())
        ON CONFLICT (user_id)
        DO UPDATE SET total_point = user_points_summary.total_point + EXCLUDED.total_point,
                      updated_at = now()
    """)
    suspend fun upsertAddPoint(userId: Long, delta: Int)
}
