package kr.jiasoft.hiteen.feature.level.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.level.domain.TierCode
import kr.jiasoft.hiteen.feature.level.domain.TierEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TierRepository : CoroutineCrudRepository<TierEntity, Long> {

    suspend fun findByTierCode(tierCode: TierCode): TierEntity

    @Query("""
        SELECT * 
        FROM tiers 
        WHERE min_points <= :exp AND max_points >= :exp
        LIMIT 1
    """)
    suspend fun findByPoints(exp: Long): TierEntity?

    @Query("SELECT * FROM tiers ORDER BY rank_order ASC")
    fun findAllOrdered(): Flow<TierEntity>


}
