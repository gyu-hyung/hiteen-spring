package kr.jiasoft.hiteen.feature.attend.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.attend.domain.AttendEntity
import kr.jiasoft.hiteen.feature.attend.infra.AttendRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.session.dto.ConsecutiveAttendDay
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class AttendService(
    private val attendRepository: AttendRepository,
    private val expService: ExpService,
    private val pointService: PointService,
) {

    /** 출석 현황 조회 */
    suspend fun consecutiveAttendDays(userId: Long): List<ConsecutiveAttendDay> {
        val days = attendRepository.findConsecutiveAttendDays(userId).toList()

        if (days.isEmpty()) return emptyList()

        val today = LocalDate.now()
        val lastAttendDate = days.maxOf { it.attendDate }

        // 📌 어제, 오늘이 아니면 끊긴 것으로 간주 → streak reset
        if (!lastAttendDate.isEqual(today.minusDays(1)) && !lastAttendDate.isEqual(today)) {
            return emptyList()
        }

        return days
    }




    /**
     * 출석하기
     */
    suspend fun attend(userId: Long, date: LocalDate? = null): Result<AttendEntity> {
        val today = date ?: LocalDate.now()

        // 오늘 이미 출석했는지 확인
        val existing = attendRepository.findByUserIdAndAttendDate(userId, today)
        if (existing != null) {
            return Result.failure(IllegalStateException("오늘은 이미 출석했어~"))
        }

        // 연속 출석일수 조회
//        val consecutiveDays = attendRepository.countConsecutiveAttendDays(user.id)
        val result = consecutiveAttendDays(userId)
        val sumDay = (result.size % 7 + 1).toShort()

        // 오늘 적용할 포인트 정책 결정 (1~7일 주기)
        val dayIndex = ((sumDay - 1) % 7 + 1)  // 1~7
        val policy = when (dayIndex) {
            1 -> PointPolicy.ATTEND_DAY1
            2 -> PointPolicy.ATTEND_DAY2
            3 -> PointPolicy.ATTEND_DAY3
            4 -> PointPolicy.ATTEND_DAY4
            5 -> PointPolicy.ATTEND_DAY5
            6 -> PointPolicy.ATTEND_DAY6
            7 -> PointPolicy.ATTEND_DAY7
            else -> PointPolicy.ETC
        }

        // 출석 기록 저장
        val attend = AttendEntity(
            userId = userId,
            attendDate = today,
            sumDay = sumDay,
            point = policy.amount,
            addPoint = when (policy) {
                PointPolicy.ATTEND_DAY7 -> 20  // 7일차 보너스
                else -> 3
            },
            createdAt = OffsetDateTime.now()
        )

        val saved = attendRepository.save(attend)

        // 포인트 지급
        pointService.applyPolicy(userId, policy, refId = saved.id)

        // 경험치 지급
        when (dayIndex) {
            7 -> expService.grantExp(userId, "ATTENDANCE", saved.id, 20)// 7일차 보너스
            else -> expService.grantExp(userId, "ATTENDANCE", saved.id, 3)
        }

        return Result.success(saved)
    }







}
