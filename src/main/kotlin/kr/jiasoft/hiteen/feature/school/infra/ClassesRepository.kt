package kr.jiasoft.hiteen.feature.school.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.school.domain.ClassesEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface ClassesRepository : ReactiveCrudRepository<ClassesEntity, Long> {
    fun findBySchoolId(schoolId: Long): Flow<ClassesEntity>

    fun findBySchoolIdAndYearAndGradeAndClassNo(
        schoolId: Long,
        year: Int,
        grade: String,
        classNo: String
    ): Mono<ClassesEntity>

    @Modifying
    @Query("UPDATE classes SET updated_id = -1")
    suspend fun markAllForDeletion(): Int

    @Modifying
    @Query("DELETE FROM classes WHERE updated_id = -1")
    suspend fun deleteMarkedForDeletion(): Int


}