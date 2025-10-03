package kr.jiasoft.hiteen.feature.point.infra

import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDate

interface PointRepository : CoroutineCrudRepository<PointEntity, Long> {
    suspend fun findAllByUserId(userId: Long): List<PointEntity>

    @Query("""
        SELECT COUNT(*) 
        FROM points 
        WHERE user_id = :userId 
          AND pointable_type = :policyCode 
          AND DATE(created_at) = :today
    """)
    suspend fun countByUserAndPolicyAndDate(
        userId: Long,
        policyCode: String,
        today: LocalDate
    ): Int

}
