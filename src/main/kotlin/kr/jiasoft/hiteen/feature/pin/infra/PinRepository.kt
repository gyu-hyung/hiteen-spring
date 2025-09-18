package kr.jiasoft.hiteen.feature.pin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PinRepository : CoroutineCrudRepository<PinEntity, Long> {
    /** 특정 사용자가 등록한 핀 목록 */
    fun findAllByUserId(userId: Long): Flow<PinEntity>

    /** 사용자에게 공개된 핀 목록 */
    fun findAllByVisibilityOrderByIdDesc(visibility: String): Flow<PinEntity>

    /** 반경(KM) 안의 전체 공개 핀 24시간 내의 데이터만 조회 */
    @Query("""
        SELECT * FROM pin
        WHERE visibility = :visibility
          AND deleted_at IS NULL
          AND created_at > now() - interval '24 hours'
          AND earth_distance(
                ll_to_earth(:lat, :lng),
                ll_to_earth(lat, lng)
              ) <= :radiusMeters
        ORDER BY id DESC
    """)
    fun findNearbyPublicPins(
        visibility: String,
        lat: Double,
        lng: Double,
        radiusMeters: Double
    ): Flow<PinEntity>

}