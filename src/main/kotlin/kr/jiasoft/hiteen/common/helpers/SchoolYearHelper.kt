package kr.jiasoft.hiteen.common.helpers

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 학년도 계산 유틸리티
 * - 3월 1일 기준으로 학년도 결정
 * - 3월 1일 이후: 해당 연도
 * - 3월 1일 이전: 전년도
 */
object SchoolYearHelper {

    /**
     * 현재 학년도를 반환합니다.
     * @param date 기준 날짜 (기본값: 현재)
     * @return 학년도 (예: 2026)
     */
    fun getSchoolYear(date: LocalDateTime? = null): Int {
        val now = date ?: LocalDateTime.now()
        val baseDate = LocalDateTime.of(now.year, 3, 1, 0, 0, 0).minusSeconds(1)

        return if (now.isAfter(baseDate)) {
            now.year
        } else {
            now.year - 1
        }
    }

    /**
     * 현재 학년도를 반환합니다. (LocalDate 버전)
     * @param date 기준 날짜 (기본값: 현재)
     * @return 학년도 (예: 2026)
     */
    fun getSchoolYear(date: LocalDate): Int {
        val startDate = LocalDate.of(date.year, 3, 1)

        return if (date.isBefore(startDate)) {
            date.year - 1
        } else {
            date.year
        }
    }

    /**
     * 현재 학년도를 반환합니다.
     * @return 학년도 (예: 2026)
     */
    fun getCurrentSchoolYear(): Int = getSchoolYear(LocalDate.now())
}

