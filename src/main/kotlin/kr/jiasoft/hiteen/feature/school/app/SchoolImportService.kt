package kr.jiasoft.hiteen.feature.school.app

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.feature.school.domain.SchoolClassesEntity
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.infra.SchoolClassesRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class SchoolImportService(
    private val schoolRepository: SchoolRepository,
    private val schoolClassesRepository: SchoolClassesRepository,
) {
    private val logger = LoggerFactory.getLogger(SchoolImportService::class.java)

    private val neisApiKey = "458d6485b25c46009758a4ab53413497"
    private val kakaoApiKey = "6ba92dcdddc5dccc78dbbaa50eb95825"

    // NEIS client
    private val neisClient = WebClient.builder()
        .baseUrl("https://open.neis.go.kr/hub")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(4 * 1024 * 1024) } // 줄임
                .build()
        )
        .clientConnector(
            ReactorClientHttpConnector(
                HttpClient.create()
                    .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .responseTimeout(Duration.ofSeconds(15))
            )
        )
        .build()

    // Kakao client
    private val kakaoClient = WebClient.builder()
        .baseUrl("https://dapi.kakao.com/v2/local")
        .defaultHeader("Authorization", "KakaoAK $kakaoApiKey")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
                .build()
        )
        .build()

    /** ✅ Kakao 주소 변환 결과 캐시 (최대 5000개, 오래된 항목 삭제) */
    private val geocodeCache = object : LinkedHashMap<String, Pair<Double?, Double?>>(5000, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Double?, Double?>>?): Boolean {
            return this.size > 5000
        }
    }

    suspend fun import() {
        logger.info("학교 + 학급 정보 가져오기 시작")

        schoolRepository.markAllForDeletion()
        schoolClassesRepository.markAllForDeletion()

        val totalCount = fetchTotalCount()
        if (totalCount <= 0) {
            logger.warn("학교 총 건수를 가져오지 못했습니다.")
            return
        }

        val pageSize = 1000
        val totalPages = (totalCount + pageSize - 1) / pageSize

        for (page in 1..totalPages) {
            neisClient.get()
                .uri { builder ->
                    builder.path("/schoolInfo")
                        .queryParam("KEY", neisApiKey)
                        .queryParam("Type", "json")
                        .queryParam("pIndex", page)
                        .queryParam("pSize", pageSize)
                        .build()
                }
                .retrieve()
                .bodyToFlux(JsonNode::class.java)
                .flatMapIterable { json ->
                    json["schoolInfo"]?.get(1)?.get("row") ?: emptyList()
                }
                .asFlow()   // 코루틴 Flow로 변환
                .onEach { row ->
                    try {
                        saveSchoolRow(row)
                    } catch (e: Exception) {
                        logger.error("학교 데이터 처리 실패: ${e.message}", e)
                    }
                }
                .collect()

            logger.info("Page $page/$totalPages 처리 완료")
        }

        schoolRepository.deleteMarkedForDeletion()
        schoolClassesRepository.deleteMarkedForDeletion()

        logger.info("학교 + 학급 정보 가져오기 완료")
    }

    private suspend fun saveSchoolRow(row: JsonNode) {
        val address = row["ORG_RDNMA"]?.asText()?.trim()
        val (lat, lng) = if (!address.isNullOrBlank()) {
            geocodeCache[address] ?: fetchLatLngFromKakao(address).also {
                geocodeCache[address] = it
            }
        } else null to null

        val entity = SchoolEntity(
            sido = row["ATPT_OFCDC_SC_CODE"]?.asText(),
            sidoName = row["ATPT_OFCDC_SC_NM"]?.asText(),
            code = row["SD_SCHUL_CODE"]?.asText() ?: UUID.randomUUID().toString(),
            name = row["SCHUL_NM"]?.asText() ?: "",
            type = resolveSchoolType(row["SCHUL_KND_SC_NM"]?.asText(), row["SCHUL_NM"]?.asText()),
            typeName = row["SCHUL_KND_SC_NM"]?.asText(),
            zipcode = row["ORG_RDNZC"]?.asText(),
            address = address,
            latitude = lat,
            longitude = lng,
            foundDate = parseDate(row["FOND_YMD"]?.asText()),
        )

//        val saved = schoolRepository.findByCode(entity.code)?.copy(
        schoolRepository.findByCode(entity.code)?.copy(
            sido = entity.sido ?: "",
            sidoName = entity.sidoName ?: "",
            name = entity.name.ifBlank { entity.name },
            type = entity.type,
            typeName = entity.typeName ?: "",
            zipcode = entity.zipcode,
            address = entity.address,
            latitude = entity.latitude,
            longitude = entity.longitude,
            foundDate = entity.foundDate,
            updatedId = 0,
            updatedAt = LocalDateTime.now()
        )?.let { schoolRepository.save(it) }
            ?: schoolRepository.save(entity.copy(updatedId = 0))

//        val classes = fetchClasses(entity.sido ?: "", entity.code, entity.name)
//        if (classes.isNotEmpty()) {
//            saveClassesForSchool(saved, classes)
//        }
    }

    private suspend fun fetchClasses(sido: String, schoolCode: String, schoolName: String): List<Map<String, String>> {
        val json = neisClient.get()
            .uri { builder ->
                builder.path("/classInfo")
                    .queryParam("KEY", neisApiKey)
                    .queryParam("Type", "json")
                    .queryParam("ATPT_OFCDC_SC_CODE", sido)
                    .queryParam("SD_SCHUL_CODE", schoolCode)
                    .queryParam("AY", LocalDate.now().year)
                    .queryParam("pIndex", 1)
                    .queryParam("pSize", 1000)
                    .build()
            }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .awaitSingleOrNull() ?: return emptyList()

        val rows = json["classInfo"]?.get(1)?.get("row")
        if (rows == null || !rows.isArray) {
            logger.debug("$schoolCode :: $schoolName :: 학급 데이터 없음")
            return emptyList()
        }

        return rows.map { row ->
            mapOf(
                "AY" to row["AY"]?.asText().orEmpty(),
                "GRADE" to row["GRADE"]?.asText().orEmpty(),
                "CLASS_NM" to row["CLASS_NM"]?.asText().orEmpty(),
                "DDDEP_NM" to row["DDDEP_NM"]?.asText().orEmpty(),
                "SCHUL_CRSE_SC_NM" to row["SCHUL_CRSE_SC_NM"]?.asText().orEmpty()
            )
        }
    }

    suspend fun saveClassesForSchool(school: SchoolEntity, classRows: List<Map<String, String>>) {
        val entities = classRows.mapNotNull { row ->
            val year = row["AY"] ?: return@mapNotNull null
            val grade = row["GRADE"] ?: return@mapNotNull null
            val classNum = row["CLASS_NM"] ?: return@mapNotNull null
            val major = row["DDDEP_NM"] ?: ""
            val schoolTypeName = row["SCHUL_CRSE_SC_NM"] ?: ""

            val className = when {
                school.typeName == "특수학교" ->
                    "$schoolTypeName ${if (schoolTypeName == "전공과") "" else grade + "학년 "}$classNum 반"
                major.isNotBlank() && major != "일반과" && major != "일반학과" ->
                    "$major $grade 학년 $classNum 반"
                else -> "$grade 학년 $classNum 반"
            }

            SchoolClassesEntity(
                code = UUID.randomUUID().toString(),
                year = year.toInt(),
                schoolId = school.id,
                schoolName = school.name,
                schoolType = school.type,
                className = className,
                major = if (major == "일반과" || major == "일반학과") "" else major,
                grade = grade,
                classNo = classNum,
                createdAt = LocalDateTime.now(),
                updatedId = 0,
            )
        }

        // 100개 단위로 잘라서 저장
        entities.chunked(100).forEach { chunk ->
            schoolClassesRepository.saveAll(chunk.asFlow()).collect()
        }

        logger.debug("${school.code} :: ${school.name} :: ${entities.size} 학급 저장")
    }


    private suspend fun fetchTotalCount(): Int {
        val json = neisClient.get()
            .uri { builder ->
                builder.path("/schoolInfo")
                    .queryParam("KEY", neisApiKey)
                    .queryParam("Type", "json")
                    .queryParam("pIndex", 1)
                    .queryParam("pSize", 1)
                    .build()
            }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .awaitSingleOrNull() ?: return 0

        return json["schoolInfo"]?.get(0)?.get("head")?.get(0)?.get("list_total_count")?.asInt() ?: 0
    }

    private fun parseDate(date: String?): LocalDate? {
        return try {
            if (date.isNullOrBlank()) null
            else LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveSchoolType(typeName: String?, fallback: String?): Int {
        val target = typeName ?: fallback ?: ""
        return when {
            target.contains("초등학교") -> 1
            target.contains("중학교") -> 2
            target.contains("고등학교") -> 3
            else -> 9
        }
    }

    private suspend fun fetchLatLngFromKakao(address: String): Pair<Double?, Double?> {
        return try {
            val json = kakaoClient.get()
                .uri { it.path("/search/address.json").queryParam("query", address).build() }
                .retrieve()
                .bodyToMono(JsonNode::class.java)
                .awaitSingleOrNull()

            val doc = json?.get("documents")?.firstOrNull()
            val lat = doc?.get("y")?.asDouble()
            val lng = doc?.get("x")?.asDouble()
            lat to lng
        } catch (e: Exception) {
            logger.warn("주소 변환 실패 ($address): ${e.message}")
            null to null
        }
    }
}
