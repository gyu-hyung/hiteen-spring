package kr.jiasoft.hiteen.feature.school.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime

@Service
class SchoolAddressImportService(
    private val schoolRepository: SchoolRepository
) {

    private val logger = LoggerFactory.getLogger(SchoolAddressImportService::class.java)

    private val kakaoKey = "6ba92dcdddc5dccc78dbbaa50eb95825" // application.yml에서 주입 권장
    private val kakaoUrl = "https://dapi.kakao.com/v2/local/search/keyword.json"

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(kakaoUrl)
        .defaultHeader("Authorization", "KakaoAK $kakaoKey")
        .build()

    /**
     * 전체 학교 주소 및 위경도 변환 Import
     */
    suspend fun importAddresses() {
        val startTime = LocalDateTime.now()
        logger.info("=============================================================")
        logger.info("School Address & Coordinates :: Import")
        logger.info("Start :: $startTime")
        logger.info("=============================================================")

        val schools = schoolRepository.findAll().toList()
        if (schools.isEmpty()) {
            logger.info("학교 데이터 없음")
            return
        }

        var total = 0
        var success = 0

        for (school in schools) {
            total++
            val schoolCode = school.code
            val schoolName = school.name

            val addr = school.address?.split(" ") ?: listOf()
            if (addr.size < 3) {
                logger.warn("$schoolCode :: $schoolName :: 주소 불완전")
                continue
            }

            val keyword = "${addr[0]} ${addr[1]} ${addr[2]} $schoolName"

            try {
                val response = webClient.get()
                    .uri { uriBuilder ->
                        uriBuilder.queryParam("query", keyword)
                            .queryParam("sort", "accuracy")
                            .build()
                    }
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .block()

                if (response == null || response["documents"] == null) {
                    logger.info("$schoolCode :: $schoolName :: 장소 데이터 없음 ($keyword)")
                    continue
                }

                @Suppress("UNCHECKED_CAST")
                val documents = response["documents"] as List<Map<String, Any>>
                if (documents.isEmpty()) {
                    logger.info("$schoolCode :: $schoolName :: 장소 데이터 없음 ($keyword)")
                    continue
                }

                // 첫 번째 결과 사용 (PHP 코드처럼 일부 학교는 두 번째 인덱스를 사용할 수도 있음)
                val row = documents[0]

                val latitude = row["y"]?.toString()
                val longitude = row["x"]?.toString()
                val roadAddress = row["road_address_name"]?.toString()
                val jibunAddress = row["address_name"]?.toString()

                if (latitude.isNullOrBlank() || longitude.isNullOrBlank()) {
                    logger.info("$schoolCode :: $schoolName :: 좌표 정보 없음 ($keyword)")
                    continue
                }

                val updated = school.copy(
                    address = roadAddress ?: jibunAddress,
                    latitude = latitude.toDouble(),
                    longitude = longitude.toDouble(),
                    updatedAt = LocalDateTime.now()
                )
                schoolRepository.save(updated)
                success++

                logger.info("$schoolCode :: $schoolName :: 좌표 변환 성공(${row["place_name"]})")

            } catch (e: Exception) {
                logger.error("$schoolCode :: $schoolName :: API 요청/파싱 실패", e)
            }
        }

        val endTime = LocalDateTime.now()
        logger.info("=============================================================")
        logger.info("총 $total 건, 성공 $success 건")
        logger.info("End :: $endTime")
        logger.info("=============================================================")
    }
}
