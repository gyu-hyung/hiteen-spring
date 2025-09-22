package kr.jiasoft.hiteen.feature.timetable.app

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.timetable.infra.TimeTableRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * 실제 NEIS API 에서 시간표 데이터를 가져와 저장하는 통합 테스트
 * ⚠️ 외부 API 호출 & DB 쓰기가 발생하므로 로컬 개발 환경에서만 실행 권장
 */
@SpringBootTest
class TimeTableImportServiceTest(
    @Autowired private val timeTableImportService: TimeTableImportService,
    @Autowired private val timeTableRepository: TimeTableRepository
) {

    @Test
    fun `NEIS API에서 시간표 데이터 가져오기`() = runBlocking {
        // when
        timeTableImportService.import()

        // then
        val count = timeTableRepository.count()
        println("저장된 시간표 개수: $count")
    }
}