package kr.jiasoft.hiteen.feature.cash.infra

import kr.jiasoft.hiteen.feature.cash.domain.CashRuleEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CashRuleRepository : CoroutineCrudRepository<CashRuleEntity, Long> {

    @Query("""
        SELECT *
        FROM cash_rules
        WHERE action_code = :actionCode
          AND deleted_at IS NULL
        LIMIT 1
    """)
    suspend fun findActiveByActionCode(actionCode: String): CashRuleEntity?
}
