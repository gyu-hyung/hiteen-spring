package kr.jiasoft.hiteen.feature.timetable.app

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.reactor.awaitSingle
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

        // ✅ 학교 단위로 스트리밍 처리
        classRepository.findDistinctSchoolIds(currentYear).collect { schoolId ->
            val school = schoolRepository.findById(schoolId) ?: return@collect

            // 학급 스트리밍 (toList 제거!)
            classRepository.findBySchoolIdAndYear(schoolId, currentYear).collect { clazz ->
                val type = when (clazz.schoolType) {
                    9 -> "spsTimetable"
                    2 -> "misTimetable"
                    else -> "hisTimetable"
                }

                try {
                    val json = client.get()
                        .uri { builder ->
                            builder.path("/$type")
                                .queryParam("KEY", apiKey)
                                .queryParam("Type", "json")
                                .queryParam("pIndex", 1)
                                .queryParam("pSize", 1000)
                                .queryParam("ATPT_OFCDC_SC_CODE", school.sido)
                                .queryParam("SD_SCHUL_CODE", school.code)
                                .queryParam("GRADE", clazz.grade)
                                .queryParam("CLASS_NM", clazz.classNo)
                                .queryParam("TI_FROM_YMD", from)
                                .queryParam("TI_TO_YMD", to)
                                .build()
                        }
                        .retrieve()
                        .bodyToMono(JsonNode::class.java)
                        .awaitSingle()

                    val rows = json[type]?.get(1)?.get("row")
                    if (rows == null || !rows.isArray || rows.isEmpty) {
                        logger.debug("${school.name} ${clazz.grade}-${clazz.classNo} :: timetable 없음")
                        return@collect
                    }

                    // ✅ DTO 변환 후 바로 버리기 (JsonNode 오래 안 들고 있음)
                    val entities = rows.map { row ->
                        TimeTableEntity(
                            classId = clazz.id,
                            year = row["AY"].asInt(),
                            semester = row["SEM"].asInt(),
                            timeDate = LocalDate.parse(row["ALL_TI_YMD"].asText(), dateFmt),
                            period = row["PERIO"].asInt(),
                            subject = row["ITRT_CNTNT"].asText()
                        )
                    }

                    // ✅ 배치 업서트 (row 단위 INSERT 지양)
                    upsertAll(entities.asFlow())

                    logger.info("${clazz.schoolName} ${clazz.grade}-${clazz.classNo}반 :: ${entities.size} rows 저장/업데이트")

                } catch (e: Exception) {
                    logger.error("${school.name} ${clazz.grade}-${clazz.classNo} error :: ${e.message}", e)
                }
            }
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
