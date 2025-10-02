package kr.jiasoft.hiteen.feature.point.app

import kr.jiasoft.hiteen.feature.point.domain.PointEntity
import kr.jiasoft.hiteen.feature.point.infra.PointRepository
import org.springframework.stereotype.Service

@Service
class PointService(
    private val pointRepository: PointRepository
) {
    suspend fun addPoints(
        userId: Long,
        amount: Int,
        type: String,
        refType: String,
        refId: Long? = null,
        memo: String? = null
    ): PointEntity {
        val point = PointEntity(
            userId = userId,
            pointableType = refType,
            pointableId = refId,
            type = "CREDIT",
            point = amount,
            memo = memo ?: "$type 적립"
        )
        return pointRepository.save(point)
    }

    suspend fun usePoints(
        userId: Long,
        amount: Int,
        refType: String,
        refId: Long? = null,
        memo: String? = null
    ): PointEntity {
        val totalPoints = getUserTotalPoints(userId)

        if (totalPoints < amount) {
            throw IllegalStateException("포인트가 부족합니다. (보유=${totalPoints}, 필요=${amount})")
        }

        val point = PointEntity(
            userId = userId,
            point = -amount,
            type = "DEBIT",
            pointableType = refType,
            pointableId = refId,
            memo = memo ?: "$refType 사용"
        )
        return pointRepository.save(point)
    }

    suspend fun getUserTotalPoints(userId: Long): Int {
        val all = pointRepository.findAllByUserId(userId)
        return all.sumOf { it.point }
    }
}
