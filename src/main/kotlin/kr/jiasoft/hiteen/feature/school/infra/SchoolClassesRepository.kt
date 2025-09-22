package kr.jiasoft.hiteen.feature.school.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.school.domain.SchoolClassesEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SchoolClassesRepository : CoroutineCrudRepository<SchoolClassesEntity, Long> {

    @Modifying
    @Query("UPDATE school_classes SET updated_id = -1")
    suspend fun markAllForDeletion(): Int

    @Modifying
    @Query("DELETE FROM school_classes WHERE updated_id = -1")
    suspend fun deleteMarkedForDeletion(): Int

    // ✅ 특정 학년도만 조회
    @Query("SELECT * FROM school_classes WHERE year = :year")
    fun findByYear(year: Int): Flow<SchoolClassesEntity>

    @Query("SELECT * FROM school_classes WHERE year = :year AND school_id = :schoolId")
    fun findBySchoolIdAndYear(schoolId: Long, year: Int): Flow<SchoolClassesEntity>

    @Query("SELECT DISTINCT school_id FROM school_classes WHERE year = :year")
    fun findDistinctSchoolIds(year: Int): Flow<Long>


}
