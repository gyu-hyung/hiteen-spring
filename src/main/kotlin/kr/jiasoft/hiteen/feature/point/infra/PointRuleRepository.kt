package kr.jiasoft.hiteen.feature.point.infra

import kr.jiasoft.hiteen.feature.point.domain.PointRuleEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface PointRuleRepository : CoroutineCrudRepository<PointRuleEntity, Long> {

    @Query("""
        SELECT *
        FROM point_rules
        WHERE action_code = :actionCode
          AND deleted_at IS NULL
        LIMIT 1
    """)
    suspend fun findActiveByActionCode(actionCode: String): PointRuleEntity?
}
