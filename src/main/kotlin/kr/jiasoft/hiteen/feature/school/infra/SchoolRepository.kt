package kr.jiasoft.hiteen.feature.school.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SchoolRepository : CoroutineCrudRepository<SchoolEntity, Long> {
    suspend fun findByCode(code: String): SchoolEntity?

    @Query("SELECT * FROM schools WHERE name ILIKE '%' || :keyword || '%'")
    fun findByNameContaining(keyword: String): Flow<SchoolEntity>
}
