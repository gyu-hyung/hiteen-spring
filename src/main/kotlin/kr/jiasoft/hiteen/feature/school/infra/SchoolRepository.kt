package kr.jiasoft.hiteen.feature.school.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SchoolRepository : CoroutineCrudRepository<SchoolEntity, Long> {
    suspend fun findByCode(code: String): SchoolEntity?

    @Query("""
        SELECT 
        (select COUNT(*) from users where school_id = s.id and deleted_at is null) memberCount , s.* 
        FROM schools s
        WHERE (:keyword IS NULL OR name ILIKE '%' || :keyword || '%')
          AND (:cursor IS NULL OR id > :cursor)
        ORDER BY id ASC
        LIMIT :limit
    """)
    fun findSchools(
        keyword: String?,
        cursor: Long?,
        limit: Int
    ): Flow<SchoolEntity>

    @Query("""
        SELECT COUNT(*) 
        FROM users 
        WHERE school_id = :schoolId 
          AND deleted_at IS NULL
    """)
    suspend fun countMembersBySchoolId(schoolId: Long): Long

}
