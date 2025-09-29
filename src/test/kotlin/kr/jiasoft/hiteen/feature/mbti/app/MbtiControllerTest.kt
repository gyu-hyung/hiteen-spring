package kr.jiasoft.hiteen.feature.mbti.app

import com.fasterxml.jackson.databind.ObjectMapper
import kr.jiasoft.hiteen.feature.mbti.config.MbtiConfig
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiAnswer
import kr.jiasoft.hiteen.feature.mbti.domain.MbtiAnswerRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@AutoConfigureWebTestClient
class MbtiControllerTest @Autowired constructor(
    private val client: WebTestClient,
    private val objectMapper: ObjectMapper,
    private val mbtiConfig: MbtiConfig,
) {

    private val token =
        "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjaGF0MSIsImlhdCI6MTc1ODg0NTQyMiwiZXhwIjoxNzU5NzA5NDIyfQ.cE6sUjQFQ0jUfSwsb4MCjddamryz1uo2TGJikY-ViRxxB_DV1xX7sfmfpRL86TaK8yKc3L3NqR4c9aP7X-dBkQ"


    @Test
    fun `질문 목록 조회 성공`() {
        client.get()
            .uri("/api/mbti/questions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data").isArray
            .jsonPath("$.data.length()").isEqualTo(mbtiConfig.questions.size)
    }

    @Test
    fun `MBTI 결과 계산 성공`() {
        // give
        val request = MbtiAnswerRequest(
            answers = listOf(
                MbtiAnswer(number = 0, answer = 5),
                MbtiAnswer(number = 1, answer = 3),
                MbtiAnswer(number = 2, answer = 4)
            )
        )

        // when & then
        client.post()
            .uri("/api/mbti/results")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(request))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.result").isNotEmpty
            .jsonPath("$.data.rates").isArray
    }

    @Test
    fun `MBTI 상세 결과 조회 성공`() {
        // given
        val mbti = "ENFP-T"

        // when & then
        val result = client.get()
            .uri { uriBuilder -> uriBuilder.path("/api/mbti/view").queryParam("mbti", mbti).build() }
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.data.result").isEqualTo(mbti)
            .jsonPath("$.data.title").isNotEmpty
            .jsonPath("$.data.description").isNotEmpty
    }
}
