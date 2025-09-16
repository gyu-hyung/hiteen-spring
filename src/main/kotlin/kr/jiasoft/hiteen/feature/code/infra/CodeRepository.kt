package kr.jiasoft.hiteen.feature.code.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CodeRepository : CoroutineCrudRepository<CodeEntity, Long> {

    @Query("SELECT * FROM codes WHERE code_group = :group AND deleted_at IS NULL")
    fun findByGroup(group: String): Flow<CodeEntity>

    @Query("SELECT code FROM codes WHERE code_group = :group ORDER BY code DESC LIMIT 1")
    suspend fun findLastCodeByGroup(group: String): String?

}
