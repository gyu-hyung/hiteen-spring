package kr.jiasoft.hiteen.feature.attend.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.attend.domain.AttendEntity
import kr.jiasoft.hiteen.feature.attend.infra.AttendRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.session.dto.ConsecutiveAttendDay
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class AttendService(
    private val attendRepository: AttendRepository,
    private val expService: ExpService,
    private val pointService: PointService,
) {

    /**
     * 출석 현황 조회 (7일 스탬프 사이클)
     * - 연속 출석이 7일을 넘으면 7일 단위로 사이클이 돌아야 한다.
     *   예) 9일 연속이면 UI에는 최근 2일만 1~2로 보이도록 반환
     * - 하루라도 빠지면 초기화(빈 목록)
     * - ✅ 추가 요구사항: 호출 시 오늘 출석을 안 했다면 자동으로 출석 처리
     */
    suspend fun consecutiveAttendDays(userId: Long): List<ConsecutiveAttendDay> {
        val today = LocalDate.now()

        // 0) 오늘 출석이 없으면 자동 출석 처리
        if (attendRepository.findByUserIdAndAttendDate(userId, today) == null) {
            // 이미 출석이면 내부에서 그대로 성공 반환됨
            attend(userId, today)
        }

        val attendedDates = attendRepository.findAttendDatesLast7Days(userId).toList().toSet()
        if (attendedDates.isEmpty()) return emptyList()

        // 1) 오늘 포함 최근 7일 내에서 '연속' 체크
        val continuousCountInWindow = (0..6)
            .takeWhile { offset ->
                attendedDates.contains(today.minusDays(offset.toLong()))
            }
            .size

        if (continuousCountInWindow == 0) return emptyList()

        // 2) 7일 스탬프 사이클 계산은 오늘 attend.sumDay(1~7)를 신뢰
        val todayAttend = attendRepository.findByUserIdAndAttendDate(userId, today)
            ?: return emptyList()

        val cycleDay = todayAttend.sumDay.toInt().coerceIn(1, 7)
        val start = today.minusDays((cycleDay - 1).toLong())

        return (0 until cycleDay).map { idx ->
            ConsecutiveAttendDay(
                attendDate = start.plusDays(idx.toLong()),
                rn = idx + 1,
            )
        }
    }


    /** 로그 */
    suspend fun logByCursor(
        userId: Long,
        cursor: String?,
        perPage: Int
    ): ApiPageCursor<AttendEntity> {

        val (cursorDate, cursorId) = parseCursor(cursor)

        val rows = attendRepository
            .findByCursor(userId, cursorDate, cursorId, perPage + 1)
            .toList()

        val hasNext = rows.size > perPage
        val items = if (hasNext) rows.dropLast(1) else rows

        val nextCursor = if (hasNext) {
            val last = items.last()
            "${last.attendDate}_${last.id}"
        } else {
            null
        }

        return ApiPageCursor(
            nextCursor = nextCursor,
            items = items,
            perPage = perPage
        )
    }

    private fun parseCursor(cursor: String?): Pair<LocalDate?, Long?> {
        if (cursor.isNullOrBlank()) return null to null

        val parts = cursor.split("_")
        return LocalDate.parse(parts[0]) to parts[1].toLong()
    }



    /**
     * 출석하기
     */
    suspend fun attend(userId: Long, date: LocalDate? = null): Result<AttendEntity> {
        val today = date ?: LocalDate.now()

        // 오늘 이미 출석했는지 확인
        val existing = attendRepository.findByUserIdAndAttendDate(userId, today)
        if (existing != null) {
            return Result.success(existing)
        }

        // sumDay는 "어제 출석의 sumDay"를 기반으로 계산(7일 사이클)
        val yesterday = today.minusDays(1)
        val yesterdayAttend = attendRepository.findByUserIdAndAttendDate(userId, yesterday)
        val sumDay = if (yesterdayAttend == null) {
            1
        } else {
            (yesterdayAttend.sumDay.toInt() % 7) + 1
        }.toShort()

        val dayIndex = sumDay.toInt().coerceIn(1, 7)
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

        val attend = AttendEntity(
            userId = userId,
            attendDate = today,
            sumDay = sumDay,
            point = policy.amount,
            addPoint = when (policy) {
                PointPolicy.ATTEND_DAY7 -> 20
                else -> 3
            },
            createdAt = OffsetDateTime.now()
        )

        val saved = attendRepository.save(attend)

        pointService.applyPolicy(userId, policy, refId = saved.id)

        when (dayIndex) {
            7 -> expService.grantExp(userId, "ATTENDANCE", saved.id, 20)
            else -> expService.grantExp(userId, "ATTENDANCE", saved.id, 3)
        }

        return Result.success(saved)
    }
}
