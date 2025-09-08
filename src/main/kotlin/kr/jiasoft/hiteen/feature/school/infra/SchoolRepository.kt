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

    // 전체 조회: 첫 페이지
    @Query("SELECT * FROM schools ORDER BY id ASC LIMIT :limit")
    fun findFirstPage(limit: Int): Flow<SchoolEntity>

    // 전체 조회: 커서 이후
    @Query("SELECT * FROM schools WHERE id > :cursor ORDER BY id ASC LIMIT :limit")
    fun findNextPage(cursor: Long, limit: Int): Flow<SchoolEntity>

    // 검색 조회: 첫 페이지
    @Query("""
        SELECT * FROM schools 
        WHERE name ILIKE '%' || :keyword || '%'
        ORDER BY id ASC LIMIT :limit
    """)
    fun findByNameContainingFirstPage(keyword: String, limit: Int): Flow<SchoolEntity>

    // 검색 조회: 커서 이후
    @Query("""
        SELECT * FROM schools 
        WHERE id > :cursor AND name ILIKE '%' || :keyword || '%'
        ORDER BY id ASC LIMIT :limit
    """)
    fun findByNameContainingNextPage(keyword: String, cursor: Long, limit: Int): Flow<SchoolEntity>

}
