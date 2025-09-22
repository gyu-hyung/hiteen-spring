package kr.jiasoft.hiteen.feature.school.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.school.infra.SchoolFoodRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class SchoolFoodImportService(
    private val schoolRepository: SchoolRepository,
    private val schoolFoodRepository: SchoolFoodRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = jacksonObjectMapper()

    private val client = WebClient.builder()
        .baseUrl("https://open.neis.go.kr/hub")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) } // 10MB 제한
                .build()
        )
        .build()

    private val apiKey = "458d6485b25c46009758a4ab53413497"
    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")




    data class MealRow(
        val MLSV_YMD: String,
        val MMEAL_SC_CODE: String,
        val MMEAL_SC_NM: String,
        val DDISH_NM: String,
        val CAL_INFO: String?
    )





    suspend fun import() = withContext(Dispatchers.IO) {
        logger.info("SchoolFood :: Import START =====================")

        val today = LocalDate.now()
        val from = today.format(dateFmt)
        val to = today.plusDays(7).format(dateFmt)

        // ✅ Stream 방식으로 처리 (메모리 절약)
        schoolRepository.findAllExcludeElementary().collect { school ->
            try {
                val json = client.get()
                    .uri { builder ->
                        builder.path("/mealServiceDietInfo")
                            .queryParam("KEY", apiKey)
                            .queryParam("Type", "json")
                            .queryParam("pIndex", 1)
                            .queryParam("pSize", 1000)
                            .queryParam("ATPT_OFCDC_SC_CODE", school.sido)
                            .queryParam("SD_SCHUL_CODE", school.code)
                            .queryParam("MLSV_FROM_YMD", from)
                            .queryParam("MLSV_TO_YMD", to)
                            .build()
                    }
                    .retrieve()
                    .bodyToMono(JsonNode::class.java)
                    .awaitSingle()

                val rows = json["mealServiceDietInfo"]?.get(1)?.get("row")
                if (rows == null || !rows.isArray || rows.isEmpty) {
                    logger.debug("${school.name} :: 0 meals")
                    return@collect
                }

                var count = 0
                for (row in rows) {
                    // ✅ JsonNode → DTO 바로 매핑
                    val dto = mapper.treeToValue(row, MealRow::class.java)

                    val mealDate = LocalDate.parse(dto.MLSV_YMD, dateFmt)
                    val meals = dto.DDISH_NM
                        .replace("<br/>", "\n")
                        .replace(Regex(" \\(\\d+(\\.\\d+)*\\)"), "")

                    // ✅ UPSERT 실행
                    schoolFoodRepository.upsert(
                        schoolId = school.id,
                        mealDate = mealDate,
                        code = dto.MMEAL_SC_CODE,
                        codeName = dto.MMEAL_SC_NM,
                        meals = meals,
                        calorie = dto.CAL_INFO
                    )

                    // ✅ 특정 학교 복사 로직
                    if (school.id == 5896L) {
                        schoolFoodRepository.upsert(
                            schoolId = 12542L,
                            mealDate = mealDate,
                            code = dto.MMEAL_SC_CODE,
                            codeName = dto.MMEAL_SC_NM,
                            meals = meals,
                            calorie = dto.CAL_INFO
                        )
                    }

                    count++
                }
                logger.info("${school.name} :: $count meals")

            } catch (e: Exception) {
                logger.error("${school.name} error :: ${e.message}", e)
            }
        }

        logger.info("SchoolFood :: Import END =====================")
    }
}
