package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminSchoolClassesResponse
import kr.jiasoft.hiteen.feature.school.domain.SchoolClassesEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminSchoolClassRepository : CoroutineCrudRepository<SchoolClassesEntity, Long> {
    @Query("""
        SELECT 
            *
        FROM school_classes c
        WHERE deleted_at IS NULL
            AND school_id = :schoolId
            AND year = :year
        ORDER BY grade ASC, class_no ASC
    """)
    suspend fun listBySchoolYear(
        schoolId: Long,
        year: Int,
    ): Flow<AdminSchoolClassesResponse>
}