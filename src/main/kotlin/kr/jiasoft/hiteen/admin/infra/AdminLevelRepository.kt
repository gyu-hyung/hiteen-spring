package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminLevelResponse
import kr.jiasoft.hiteen.feature.level.domain.TierEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminLevelRepository : CoroutineCrudRepository<TierEntity, Long> {
    @Query("""
        SELECT 
            t.*,
            (SELECT COUNT(*) FROM users WHERE tier_id = t.id AND deleted_at IS NULL) member_count
        FROM tiers t
        WHERE t.deleted_at IS NULL AND (:search IS NULL OR t.tier_name_kr ILIKE '%' || :search || '%')
        ORDER BY t.level ASC, t.division_no ASC, t.rank_order ASC
    """)
    suspend fun listLevels(
        search: String?,
    ): Flow<AdminLevelResponse>

    // 레벨에 해당하는 회원수 체크
    // 해당 레벨의 회원이 있으면 삭제하지 못하게 하기 위해..
    @Query("""
        SELECT COUNT(*)
        FROM tiers
        WHERE deleted_at IS NULL AND tier_id = :tierId
    """)
    suspend fun countLevelUsers(tierId: Long): Int
}