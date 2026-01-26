package kr.jiasoft.hiteen.feature.dashboard.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.dashboard.domain.DashboardStatisticsEntity
import kr.jiasoft.hiteen.feature.dashboard.dto.RegionStatRaw
import kr.jiasoft.hiteen.feature.dashboard.dto.SchoolTypeStatRaw
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface DashboardAggregateRepository : CoroutineCrudRepository<DashboardStatisticsEntity, Long> {

    @Query("""
        SELECT s.type as type, COUNT(u.id) as count
        FROM users u
        JOIN schools s ON u.school_id = s.id
        WHERE u.deleted_at IS NULL
          AND COALESCE(u.role, '') <> 'ADMIN'
        GROUP BY s.type
    """)
    fun getSchoolTypeStats(): Flow<SchoolTypeStatRaw>

    @Query("""
        SELECT s.sido as sido, COUNT(u.id) as count
        FROM users u
        JOIN schools s ON u.school_id = s.id
        WHERE u.deleted_at IS NULL
          AND COALESCE(u.role, '') <> 'ADMIN'
        GROUP BY s.sido
    """)
    fun getRegionStats(): Flow<RegionStatRaw>

    @Query("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND COALESCE(role, '') <> 'ADMIN'")
    suspend fun getTotalUserCount(): Long

    @Query("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND created_at >= CURRENT_DATE AND COALESCE(role, '') <> 'ADMIN'")
    suspend fun getTodayJoinCount(): Long

    @Query("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND created_at >= DATE_TRUNC('month', CURRENT_DATE) AND COALESCE(role, '') <> 'ADMIN'")
    suspend fun getMonthJoinCount(): Long

    @Query("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND created_at >= CURRENT_DATE - INTERVAL '1 day' AND created_at < CURRENT_DATE AND COALESCE(role, '') <> 'ADMIN'")
    suspend fun getYesterdayJoinCount(): Long

    @Query("""
        SELECT COUNT(*) FROM users 
        WHERE deleted_at IS NULL 
          AND COALESCE(role, '') <> 'ADMIN'
          AND created_at >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month') 
          AND created_at < DATE_TRUNC('month', CURRENT_DATE)
    """)
    suspend fun getLastMonthJoinCount(): Long
}
