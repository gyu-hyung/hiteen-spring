package kr.jiasoft.hiteen.feature.school.app

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kr.jiasoft.hiteen.feature.school.domain.ClassesEntity
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.domain.SchoolInfoResponse
import kr.jiasoft.hiteen.feature.school.domain.SchoolRow
import kr.jiasoft.hiteen.feature.school.infra.ClassesRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class SchoolImportService(
    private val schoolRepository: SchoolRepository,
    private val classesRepository: ClassesRepository,
) {
    private val logger = LoggerFactory.getLogger(SchoolImportService::class.java)

    private val neisApiKey = "458d6485b25c46009758a4ab53413497"
    private val kakaoApiKey = "6ba92dcdddc5dccc78dbbaa50eb95825"

    // NEIS client
    private val neisClient = WebClient.builder()
        .baseUrl("https://open.neis.go.kr/hub")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
                .build()
        )
        .build()

    // Kakao client
    private val kakaoClient = WebClient.builder()
        .baseUrl("https://dapi.kakao.com/v2/local")
        .defaultHeader("Authorization", "KakaoAK $kakaoApiKey")
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs

                { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
                .build()
        )
        .build()

    suspend fun fetchAndSaveSchools() {
        logger.info("학교 + 학급 정보 가져오기 시작")

        // 1. 전체 updated_id 초기화 (-1)
        schoolRepository.markAllForDeletion()
        classesRepository.markAllForDeletion()


        val totalCount = fetchTotalCount()
        if (totalCount <= 0) {
            logger.warn("학교 총 건수를 가져오지 못했습니다.")
            return
        }

        val pageSize = 1000
        val totalPages = (totalCount + pageSize - 1) / pageSize

        for (page in 1..totalPages) {
            val response = neisClient.get()
                .uri { builder ->
                    builder.path("/schoolInfo")
                        .queryParam("KEY", neisApiKey)
                        .queryParam("Type", "json")
                        .queryParam("pIndex", page)
                        .queryParam("pSize", pageSize)
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .awaitSingleOrNull() ?: continue

            val result: SchoolInfoResponse = try {
                jacksonObjectMapper().readValue(response)
            } catch (e: Exception) {
                logger.error("JSON 파싱 실패 (page=$page): ${e.message}")
                continue
            }

            val rows = result.schoolInfo.getOrNull(1)?.row ?: emptyList()
            for (row in rows) {
                saveSchool(row)
            }
        }

        // 3. 아직 -1 로 남은 데이터 삭제
        schoolRepository.deleteMarkedForDeletion()
        classesRepository.deleteMarkedForDeletion()

        logger.info("학교 + 학급 정보 가져오기 완료")
    }

    private suspend fun saveSchool(row: SchoolRow) {
        val address = row.ORG_RDNMA?.trim()
        var latitude: Double? = null
        var longitude: Double? = null

        if (!address.isNullOrBlank()) {
            try {
                val response = kakaoClient.get()
                    .uri { it.path("/search/address.json").queryParam("query", address).build() }
                    .header("Authorization", "KakaoAK $kakaoApiKey") // 🔑 반드시 헤더 추가
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .awaitSingleOrNull()

                if (response != null) {
                    val json: JsonNode = jacksonObjectMapper().readTree(response)
                    val documents = json["documents"]
                    if (documents != null && documents.isArray && documents.size() > 0) {
                        latitude = documents[0]["y"]?.asDouble()
                        longitude = documents[0]["x"]?.asDouble()
                    }
                }
            } catch (e: Exception) {
                logger.warn("${row.SCHUL_NM} :: Kakao 주소 변환 실패 (${e.message})")
            }
        }

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
            address = address,
            latitude = latitude,
            longitude = longitude,
            foundDate = parseDate(row.FOND_YMD),
        )

        val existing = schoolRepository.findByCode(entity.code)
        val saved = if (existing != null) {
            // 변경된 값만 갱신
            val updated = existing.copy(
                sido = entity.sido ?: existing.sido,
                sidoName = entity.sidoName ?: existing.sidoName,
                name = entity.name.ifBlank { existing.name },
                type = entity.type,
                typeName = entity.typeName ?: existing.typeName,
                zipcode = entity.zipcode ?: existing.zipcode,
                address = entity.address ?: existing.address,
                latitude = entity.latitude ?: existing.latitude,
                longitude = entity.longitude ?: existing.longitude,
                foundDate = entity.foundDate ?: existing.foundDate,
                updatedId = 0,
                updatedAt = LocalDateTime.now()
            )
            schoolRepository.save(updated)
        } else {
            // 신규 insert
            schoolRepository.save(entity.copy(updatedId = 0))
        }


        logger.info("저장 완료: ${saved.id} - ${saved.name}")

        // 학급 정보까지 저장
        val classes = fetchClasses(row.ATPT_OFCDC_SC_CODE, row.SD_SCHUL_CODE, row.SCHUL_NM)
        if (classes.isNotEmpty()) {
            saveClassesForSchool(saved, classes)
        }

    }


    private suspend fun fetchClasses(sido: String, schoolCode: String, schoolName: String): List<Map<String, String>> {
        val response = neisClient.get()
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
            .bodyToMono(String::class.java)
            .awaitSingleOrNull()

        if (response.isNullOrBlank()) {
            logger.warn("$schoolCode :: $schoolName :: 학급 데이터 추출 실패 (응답 없음)")
            return emptyList()
        }

        return try {
            val json = jacksonObjectMapper().readTree(response)
            val rows = json["classInfo"]?.get(1)?.get("row")
            if (rows != null && rows.isArray) {
                jacksonObjectMapper().convertValue(rows, object : TypeReference<List<Map<String, String>>>() {})
            } else {
                logger.info("$schoolCode :: $schoolName :: 학급 데이터 없음")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("$schoolCode :: $schoolName :: 학급 데이터 파싱 실패", e)
            emptyList()
        }

    }


    suspend fun saveClassesForSchool(school: SchoolEntity, classRows: List<Map<String, String>>) {
        var count = 0

        for (row in classRows) {
            val year = row["AY"] ?: continue
            val grade = row["GRADE"] ?: continue
            val classNum = row["CLASS_NM"] ?: continue
            val major = row["DDDEP_NM"] ?: ""
            val schoolTypeName = row["SCHUL_CRSE_SC_NM"] ?: ""

            // 학급명 구성
            val className = when {
                school.typeName == "특수학교" ->
                    "$schoolTypeName ${if (schoolTypeName == "전공과") "" else grade + "학년 "}$classNum 반"
                major.isNotBlank() && major != "일반과" && major != "일반학과" ->
                    "$major $grade 학년 $classNum 반"
                else ->
                    "$grade 학년 $classNum 반"
            }



            val existing = classesRepository.findBySchoolIdAndYearAndGradeAndClassNo(
                school.id, year.toInt(), grade, classNum
            ).awaitSingleOrNull()

            val entity = existing?.copy(
                className = className,
                major = if (major == "일반과" || major == "일반학과") "" else major,
                updatedId = 0,
                updatedAt = LocalDateTime.now()
            ) ?: ClassesEntity(
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

            classesRepository.save(entity).awaitSingle()


            logger.info("학급 저장 완료: ${entity.id} - ${entity.className}")
            count++

        }

        logger.info("${school.code} :: ${school.name} :: ${count} 학급 성공")
    }


    private suspend fun fetchTotalCount(): Int {
        val response = neisClient.get()
            .uri { builder ->
                builder.path("/schoolInfo")
                    .queryParam("KEY", neisApiKey)
                    .queryParam("Type", "json")
                    .queryParam("pIndex", 1)
                    .queryParam("pSize", 1)
                    .build()
            }
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingleOrNull() ?: return 0

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
