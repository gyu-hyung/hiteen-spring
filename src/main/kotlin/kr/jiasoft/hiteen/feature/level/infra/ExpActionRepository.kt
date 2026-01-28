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

    @Query(
        """
        SELECT *
        FROM exp_actions
        WHERE (:enabled IS NULL OR enabled = :enabled)
        ORDER BY
            CASE WHEN :order = 'ASC' THEN action_code END ASC,
            CASE WHEN :order = 'DESC' THEN action_code END DESC
        LIMIT :size OFFSET :offset
    """
    )
    fun listByPage(
        enabled: Boolean?,
        order: String,
        size: Int,
        offset: Long,
    ): Flow<ExpActionEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM exp_actions
        WHERE (:enabled IS NULL OR enabled = :enabled)
    """
    )
    suspend fun totalCount(enabled: Boolean?): Int

    @Query("""
        UPDATE exp_actions
        SET enabled = false,
            updated_at = CURRENT_TIMESTAMP
        WHERE action_code = :actionCode
    """)
    suspend fun disableByActionCode(actionCode: String): Int
}
