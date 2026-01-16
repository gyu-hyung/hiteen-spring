package kr.jiasoft.hiteen.feature.level.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.level.domain.ExpActionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ExpActionRepository : CoroutineCrudRepository<ExpActionEntity, Long> {

    @Query("""
        SELECT *
        FROM exp_actions
        WHERE action_code = :actionCode
        LIMIT 1
    """)
    suspend fun findByActionCode(actionCode: String): ExpActionEntity?

    @Query("""
        SELECT *
        FROM exp_actions
        WHERE (:enabled IS NULL OR enabled = :enabled)
        ORDER BY action_code ASC
    """)
    fun findAllByEnabled(enabled: Boolean? = null): Flow<ExpActionEntity>

    @Query("""
        UPDATE exp_actions
        SET enabled = false,
            updated_at = CURRENT_TIMESTAMP
        WHERE action_code = :actionCode
    """)
    suspend fun disableByActionCode(actionCode: String): Int
}

