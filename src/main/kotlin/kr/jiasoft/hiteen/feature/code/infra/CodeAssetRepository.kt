package kr.jiasoft.hiteen.feature.code.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.code.domain.CodeAssetEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CodeAssetRepository : CoroutineCrudRepository<CodeAssetEntity, Long> {
    @Query("SELECT * FROM code_assets WHERE code_id = :codeId")
    fun findByCodeId(codeId: Long): Flow<CodeAssetEntity>
}
