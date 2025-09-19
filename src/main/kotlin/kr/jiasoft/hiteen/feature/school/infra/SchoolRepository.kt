package kr.jiasoft.hiteen.feature.school.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import org.springframework.data.r2dbc.repository.Modifying
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


    /**
     * 초등학교(type = 1) 제외한 모든 학교 조회
     */
    @Query("""
        SELECT * 
        FROM schools
        WHERE type <> '1'
    """)
    fun findAllExcludeElementary(): Flow<SchoolEntity>


    @Modifying
    @Query("UPDATE schools SET updated_id = -1")
    suspend fun markAllForDeletion(): Int

    @Modifying
    @Query("DELETE FROM schools WHERE updated_id = -1")
    suspend fun deleteMarkedForDeletion(): Int



}
