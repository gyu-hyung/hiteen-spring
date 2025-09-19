package kr.jiasoft.hiteen.feature.school.app

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.school.infra.SchoolFoodRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class SchoolFoodImportService(
    private val schoolRepository: SchoolRepository,
    private val schoolFoodRepository: SchoolFoodRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val client = WebClient.builder().baseUrl("https://open.neis.go.kr/hub").build()
    private val mapper = jacksonObjectMapper()

    private val apiKey = "458d6485b25c46009758a4ab53413497"

    suspend fun import() = withContext(Dispatchers.IO) {
        logger.info("SchoolFood :: Import START =====================")

        val schools = schoolRepository.findAllExcludeElementary().toList()
        val today = LocalDate.now()
        val from = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val to = today.plusDays(7).format(DateTimeFormatter.ofPattern("yyyyMMdd"))

        for (school in schools) {
            try {
                val uri = client.get()
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
                    .bodyToMono(String::class.java)
                    .awaitSingle()

                val json: JsonNode = mapper.readTree(uri)
                val rows = json.path("mealServiceDietInfo").get(1)?.path("row")

                if (rows == null || !rows.isArray || rows.isEmpty) {
                    logger.info("${school.name} :: 0 meals")
                    continue
                }

                var count = 0
                for (row in rows) {
                    val code = row["MMEAL_SC_CODE"].asText()
                    val codeName = row["MMEAL_SC_NM"].asText()
                    val mealDate = LocalDate.parse(row["MLSV_YMD"].asText(), DateTimeFormatter.ofPattern("yyyyMMdd"))
                    val meals = row["DDISH_NM"].asText()
                        .replace("<br/>", "\n")
                        .replace(Regex(" \\(\\d+(\\.\\d+)*\\)"), "")
                    val calorie = row["CAL_INFO"].asText()

                    schoolFoodRepository.upsert(
                        schoolId = school.id!!,
                        mealDate = mealDate,
                        code = code,
                        codeName = codeName,
                        meals = meals,
                        calorie = calorie
                    )

                    // 특정 학교 복사 로직 (5896 → 12542)
                    if (school.id == 5896L) {
                        schoolFoodRepository.upsert(
                            schoolId = 12542L,
                            mealDate = mealDate,
                            code = code,
                            codeName = codeName,
                            meals = meals,
                            calorie = calorie
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
