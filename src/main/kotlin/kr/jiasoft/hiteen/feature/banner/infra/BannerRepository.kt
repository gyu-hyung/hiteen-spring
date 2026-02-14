package kr.jiasoft.hiteen.feature.banner.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.banner.domain.BannerEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BannerRepository : CoroutineCrudRepository<BannerEntity, Long> {

    @Query("""
        SELECT b.*
        FROM banners b
        WHERE b.deleted_at IS NULL
            AND b.status = 'ACTIVE'
            AND (:category IS NULL OR b.category = :category)
            AND (b.start_date IS NULL OR b.start_date <= CURRENT_DATE)
            AND (b.end_date IS NULL OR b.end_date >= CURRENT_DATE)
        ORDER BY a.created_at DESC
    """)
    fun listActive(category: String?): Flow<BannerEntity>
}

