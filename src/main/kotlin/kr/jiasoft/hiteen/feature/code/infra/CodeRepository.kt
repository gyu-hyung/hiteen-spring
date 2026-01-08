package kr.jiasoft.hiteen.feature.code.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import kr.jiasoft.hiteen.feature.code.dto.CodeWithAssetResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import reactor.core.publisher.Flux

interface CodeRepository : CoroutineCrudRepository<CodeEntity, Long> {

    @Cacheable(cacheNames = ["code"], key = "#group")
    @Query("""
        SELECT c.*, ca.uid
        FROM codes c
        left join code_assets ca on c.id = ca.code_id 
        WHERE deleted_at IS null
        AND (:group IS NULL OR c.code_group = :group)
        ORDER BY code_group, code
    """)
    fun findByGroup(group: String?): Flux<CodeWithAssetResponse>

    @Query("SELECT code FROM codes WHERE code_group = :group ORDER BY code DESC LIMIT 1")
    suspend fun findLastCodeByGroup(group: String): String?

}
