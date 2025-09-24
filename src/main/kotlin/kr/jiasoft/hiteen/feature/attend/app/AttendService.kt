package kr.jiasoft.hiteen.feature.attend.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.attend.domain.AttendEntity
import kr.jiasoft.hiteen.feature.attend.infra.AttendRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class AttendService(
    private val attendRepository: AttendRepository,
    private val expService: ExpService
) {

    /** 출석 현황 조회 */
    suspend fun view(user: UserEntity): List<AttendEntity> {
        return attendRepository.findAllByUserId(user.id).toList()
    }

    /** 출석하기 */
    suspend fun attend(user: UserEntity): Result<AttendEntity> {
        val today = LocalDate.now()

        // 오늘 이미 출석했는지 확인
        val existing = attendRepository.findByUserIdAndAttendDate(user.id, today)
        if (existing != null) {
            return Result.failure(IllegalStateException("오늘은 이미 출석했어~"))
        }

        // 이전 출석일수
        val histories = attendRepository.findAllByUserId(user.id).toList()
        val sumDay = (histories.size + 1).toShort()

        // 포인트 & 경험치 계산
        //TODO 포인트 상수화
        val (point, addPoint) = when (sumDay % 7) {
            0 -> 500 to 20   // Day 7 보너스
            else -> 100 to 3 // Day 1~6
        }

        val attend = AttendEntity(
            userId = user.id,
            attendDate = today,
            sumDay = sumDay,
            point = point,
            addPoint = addPoint,
            createdAt = OffsetDateTime.now()
        )

        val saved = attendRepository.save(attend)


        expService.grantExp(user.id, "ATTENDANCE", saved.id)

        return Result.success(saved)
    }
}
