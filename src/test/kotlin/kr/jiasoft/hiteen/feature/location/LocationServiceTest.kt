package kr.jiasoft.hiteen.feature.location

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import kotlinx.coroutines.runBlocking

import reactor.core.publisher.Mono
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper




class LocationServiceTest {

    private val locationHistoryRepository = mock<LocationHistoryRepository>()
    private val objectMapper = jacksonObjectMapper()


    private val mongoTemplate = mock<org.springframework.data.mongodb.core.ReactiveMongoTemplate>() // 사용 안해도 OK

    private val locationService = LocationService(mongoTemplate, locationHistoryRepository, objectMapper)

    @Test
    fun `정상적인 JSON이면 LocationHistory 저장`() {
        runBlocking {
            // given
            val json = """
            {
                "userId": "userId123",
                "lat": 37.123,
                "lng": 127.456,
                "timestamp": 1722432318000
            }
        """.trimIndent()
            val entity = LocationHistory(
                userId = "userId123",
                lat = 37.123,
                lng = 127.456,
                timestamp = 1722432318000
            )

            whenever(locationHistoryRepository.save(any())).thenReturn(Mono.just(entity))


            // when
            locationService.saveLocationAsyncFromJson(json)

            // then
            verify(locationHistoryRepository, times(1)).save(argThat {
                this.userId == "userId123"
                        && this.lat == 37.123
                        && this.lng == 127.456
                        && this.timestamp == 1722432318000
            })
        }
    }

    @Test
    fun `잘못된 JSON이면 IllegalArgumentException 발생`() = runBlocking {
        // given
        val badJson = """{ "userId123": "userId123", "latitude": "bad", "longitude": 127.456 }"""

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                locationService.saveLocationAsyncFromJson(badJson)
            }
        }
        assertTrue(exception.message!!.contains("Invalid location payload"))
    }
}
