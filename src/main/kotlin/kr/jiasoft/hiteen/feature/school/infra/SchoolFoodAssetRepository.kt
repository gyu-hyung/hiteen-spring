package kr.jiasoft.hiteen.feature.school.infra

import kr.jiasoft.hiteen.feature.school.domain.SchoolFoodAssetEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SchoolFoodAssetRepository : CoroutineCrudRepository<SchoolFoodAssetEntity, Long> {

    @Query("""
        SELECT * FROM school_food_image 
        WHERE school_id = :schoolId
        AND year = :year
        AND month = :month
        AND status = 1
        AND deleted_at IS NULL
        ORDER BY created_at DESC
        LIMIT 1
    """)
    suspend fun findLatest(schoolId: Long, year: Int, month: Int): SchoolFoodAssetEntity?

    @Query("""
        UPDATE school_food_image 
        SET report_count = report_count + 1, updated_at = now()
        WHERE id = :id
    """)
    suspend fun incrementReport(id: Long): Int
}

