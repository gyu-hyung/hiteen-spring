package kr.jiasoft.hiteen.feature.code.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import kr.jiasoft.hiteen.feature.code.dto.CodeWithAssetResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CodeRepository : CoroutineCrudRepository<CodeEntity, Long> {

    @Query("""
        SELECT c.*, ca.uid
        FROM codes c
        left join code_assets ca on c.id = ca.code_id 
        WHERE deleted_at IS null
        AND c.code_group = :group
        ORDER BY code_group, code
    """)
    fun findByGroup(group: String): Flow<CodeWithAssetResponse>

    @Query("SELECT code FROM codes WHERE code_group = :group ORDER BY code DESC LIMIT 1")
    suspend fun findLastCodeByGroup(group: String): String?

}
