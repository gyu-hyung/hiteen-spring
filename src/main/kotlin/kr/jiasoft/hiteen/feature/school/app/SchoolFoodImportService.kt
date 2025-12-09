package kr.jiasoft.hiteen.feature.school.app

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.school.domain.NeisProperties
import kr.jiasoft.hiteen.feature.school.infra.SchoolFoodRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.measureTime

@Service
class SchoolFoodImportService(
    private val schoolRepository: SchoolRepository,
    private val schoolFoodRepository: SchoolFoodRepository,
    private val props: NeisProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)


    private val client = WebClient.builder()
        .baseUrl(props.baseUrl)
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) } // 10MB 제한
                .build()
        )
        .build()

    private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd")



    @JsonIgnoreProperties(ignoreUnknown = true)
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

        schoolRepository.findAllByOrderByIdAsc().collect { school ->

            val totalTime = measureTime {

                try {
                    lateinit var json: JsonNode

                    val apiTime = measureTime {
                        json = client.get()
                            .uri { builder ->
                                builder.path("/mealServiceDietInfo")
                                    .queryParam("KEY", props.apiKey)
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
                    }

                    logger.info("📡 ${school.name} API Response Time: ${apiTime.inWholeMilliseconds}ms")

                    val rows = json["mealServiceDietInfo"]?.get(1)?.get("row")
                    if (rows == null || !rows.isArray || rows.isEmpty) {
                        logger.debug("${school.name} :: 0 meals")
                        return@collect
                    }

                    val dbTime = measureTime {
                        var count = 0
                        for (row in rows) {
                            val dto = objectMapper.treeToValue(row, MealRow::class.java)

                            val mealDate = LocalDate.parse(dto.MLSV_YMD, dateFmt)
                            val meals = dto.DDISH_NM
                                .replace("<br/>", "\n")
                                .replace(Regex(" \\(\\d+(\\.\\d+)*\\)"), "")

                            schoolFoodRepository.upsert(
                                schoolId = school.id,
                                mealDate = mealDate,
                                code = dto.MMEAL_SC_CODE,
                                codeName = dto.MMEAL_SC_NM,
                                meals = meals,
                                calorie = dto.CAL_INFO
                            )
                            count++
                        }
                        logger.info("${school.name} :: $count meals saved")
                    }

                    logger.info("💾 ${school.name} DB Write Time: ${dbTime.inWholeMilliseconds}ms")

                } catch (e: Exception) {
                    logger.error("${school.name} error :: ${e.message}", e)
                }
            }

            logger.info("⏳ Total Processing Time for ${school.id}  ${school.name}: ${totalTime.inWholeMilliseconds}ms\n")
        }

        logger.info("SchoolFood :: Import END =====================")
    }


}
