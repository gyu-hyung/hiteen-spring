package kr.jiasoft.hiteen.feature.school

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.domain.SchoolInfoResponse
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * TODO kakao api로 학교 좌표 변환
 */
@Component
class SchoolImportService(
    private val schoolRepository: SchoolRepository,
) {

    private val logger = LoggerFactory.getLogger(SchoolImportService::class.java)


    private val apiKey = "458d6485b25c46009758a4ab53413497"

    private val client = WebClient.builder()
        .baseUrl("https://open.neis.go.kr/hub")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { config ->
                    config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) // 10MB
                }
                .build()
        )
        .build()


    suspend fun fetchAndSaveSchools() {
        val start = System.currentTimeMillis()
        logger.info("학교 정보 가져오기 시작")

        // 먼저 total count 확인
        val totalCount = fetchTotalCount()
        if (totalCount <= 0) {
            logger.warn("학교 총 건수를 가져오지 못했습니다.")
            return
        }
        val pageSize = 1000
        val totalPages = (totalCount + pageSize - 1) / pageSize
        logger.info("총 학교 수: $totalCount, 총 페이지 수: $totalPages")

        var savedCount = 0

        for (page in 1..totalPages) {
            val response = client.get()
                .uri { builder ->
                    builder.path("/schoolInfo")
                        .queryParam("KEY", apiKey)
                        .queryParam("Type", "json")
                        .queryParam("pIndex", page)
                        .queryParam("pSize", pageSize)
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .awaitSingleOrNull()

            if (response == null) {
                logger.error("학교 API 응답 없음 (page=$page)")
                continue
            }

            val mapper = jacksonObjectMapper()
            val result: SchoolInfoResponse = try {
                mapper.readValue(response)
            } catch (e: Exception) {
                logger.error("JSON 파싱 실패 (page=$page): ${e.message}")
                continue
            }

            val rows = result.schoolInfo.getOrNull(1)?.row ?: emptyList()
            logger.info("page=$page, 받은 학교 수: ${rows.size}")

            for (row in rows) {
                val entity = SchoolEntity(
                    sido = row.ATPT_OFCDC_SC_CODE,
                    sidoName = row.ATPT_OFCDC_SC_NM,
                    code = row.SD_SCHUL_CODE,
                    name = row.SCHUL_NM,
                    type = when {
                        row.SCHUL_KND_SC_NM?.contains("초등") == true -> 1
                        row.SCHUL_KND_SC_NM?.contains("중") == true -> 2
                        row.SCHUL_KND_SC_NM?.contains("고") == true -> 3
                        else -> 9
                    },
                    typeName = row.SCHUL_KND_SC_NM,
                    zipcode = row.ORG_RDNZC?.trim(),
                    address = row.ORG_RDNMA?.trim(),
                    latitude = null,
                    longitude = null,
                    foundDate = parseDate(row.FOND_YMD),
                )

                val saved = schoolRepository.save(entity)
                savedCount++
                logger.info("저장 완료: ${saved.id} - ${saved.name}")
            }
        }

        val end = System.currentTimeMillis()
        val elapsed = (end - start) / 1000.0
        logger.info("총 저장 개수: $savedCount")
        logger.info("학교 정보 가져오기 종료 (걸린 시간: ${elapsed}초)")
    }

    /**
     * 총 건수 조회
     */
    private suspend fun fetchTotalCount(): Int {
        val response = client.get()
            .uri { builder ->
                builder.path("/schoolInfo")
                    .queryParam("KEY", apiKey)
                    .queryParam("Type", "json")
                    .queryParam("pIndex", 1)
                    .queryParam("pSize", 1)
                    .build()
            }
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingleOrNull()

        if (response == null) return 0

        return try {
            val json = jacksonObjectMapper().readTree(response)
            json["schoolInfo"]?.get(0)?.get("head")?.get(0)?.get("list_total_count")?.asInt() ?: 0
        } catch (e: Exception) {
            logger.error("총 건수 파싱 실패: ${e.message}")
            0
        }
    }


    private fun parseDate(date: String?): LocalDate? {
        return try {
            if (date.isNullOrBlank()) null
            else LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } catch (e: Exception) {
            null
        }
    }
}
