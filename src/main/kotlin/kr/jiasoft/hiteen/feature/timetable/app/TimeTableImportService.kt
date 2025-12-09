package kr.jiasoft.hiteen.feature.timetable.app

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.feature.school.domain.SchoolClassesEntity
import kr.jiasoft.hiteen.feature.school.infra.SchoolClassesRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import kr.jiasoft.hiteen.feature.timetable.domain.TimeTableEntity
import kr.jiasoft.hiteen.feature.timetable.infra.TimeTableRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class TimeTableImportService(
    private val classRepository: SchoolClassesRepository,
    private val schoolRepository: SchoolRepository,
    private val timeTableRepository: TimeTableRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val client = WebClient.builder()
        .baseUrl("https://open.neis.go.kr/hub")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) } // ✅ 응답 버퍼 줄임
                .build()
        )
        .build()

    private val apiKey = "458d6485b25c46009758a4ab53413497"
    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")

    suspend fun import() {
        logger.info("TimeTable :: Import START =====================")

        val today = LocalDate.now()
        val currentYear = today.year
        val from = today.format(dateFmt)
        val to = today.plusDays(7).format(dateFmt)

        classRepository.findDistinctSchoolIds(currentYear).collect { schoolId ->

            val school = schoolRepository.findById(schoolId) ?: return@collect

            // ✅ grade → (classNo → classEntity)
            val gradeClassMap = mutableMapOf<String, MutableMap<String, SchoolClassesEntity>>()

            classRepository
                .findBySchoolIdAndYear(schoolId, currentYear)
                .collect { clazz ->
                    gradeClassMap
                        .getOrPut(clazz.grade) { mutableMapOf() }[clazz.classNo] = clazz
                }

            for ((grade, classMap) in gradeClassMap) {

                val type = when (school.type) {
                    1 -> "elsTimetable"
                    2 -> "misTimetable"
                    3 -> "hisTimetable"
                    9 -> "spsTimetable"
                    else -> continue
                }

                var page = 1
                val pageSize = 1000

                while (true) {
                    try {
                        val json = client.get()
                            .uri { builder ->
                                builder.path("/$type")
                                    .queryParam("KEY", apiKey)
                                    .queryParam("Type", "json")
                                    .queryParam("pIndex", page)
                                    .queryParam("pSize", pageSize)
                                    .queryParam("ATPT_OFCDC_SC_CODE", school.sido)
                                    .queryParam("SD_SCHUL_CODE", school.code)
                                    .queryParam("GRADE", grade)
                                    .queryParam("TI_FROM_YMD", from)
                                    .queryParam("TI_TO_YMD", to)
                                    .build()
                            }
                            .retrieve()
                            .bodyToMono(JsonNode::class.java)
                            .awaitSingle()

                        val rows = json[type]?.get(1)?.get("row")
                        if (rows == null || !rows.isArray || rows.isEmpty) {
                            break // ✅ 더 이상 데이터 없음 → 페이지 루프 종료
                        }

                        rows.forEach { row ->
                            val classNo = row["CLASS_NM"]?.asText() ?: return@forEach
                            val clazz = classMap[classNo] ?: return@forEach

                            val subject =
                                if (row["ITRT_CNTNT"]?.asText().isNullOrBlank() ||
                                    row["ITRT_CNTNT"]?.asText() == "null"
                                ) "-"
                                else row["ITRT_CNTNT"]?.asText()!!

                            timeTableRepository.upsert(
                                classId = clazz.id,
                                year = row["AY"].asInt(),
                                semester = row["SEM"].asInt(),
                                timeDate = LocalDate.parse(row["ALL_TI_YMD"].asText(), dateFmt),
                                period = row["PERIO"].asInt(),
                                subject = subject,
                            )
                        }

                        // ✅ 마지막 페이지 판단
                        if (rows.size() < pageSize) break

                        page++  // ✅ 다음 페이지

                    } catch (e: Exception) {
                        logger.error("${school.name} ${grade}학년 page=$page error :: ${e.message}", e)
                        break
                    }
                }

                logger.info("${school.name} ${grade}학년 :: 전체 페이지 처리 완료")
            }

            // ✅ 한 학교 처리 끝나면 메모리 정리
            gradeClassMap.clear()
        }

        logger.info("TimeTable :: Import END =====================")
    }




    /**
     * ✅ R2DBC는 기본 upsertAll을 지원 안 하므로, saveAll로 대체하거나
     * Repository에 @Query + ON CONFLICT 구현 필요
     */
    private suspend fun upsertAll(flow: Flow<TimeTableEntity>) {
        flow.collect { entity ->
            timeTableRepository.upsert(
                classId = entity.classId,
                year = entity.year,
                semester = entity.semester,
                timeDate = entity.timeDate,
                period = entity.period,
                subject = entity.subject
            )
        }
    }

}
