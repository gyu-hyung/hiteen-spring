package kr.jiasoft.hiteen.feature.location

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactive.awaitFirst
import kr.jiasoft.hiteen.feature.mqtt.MqttResponse

@Service
class LocationService(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val locationHistoryMongoRepository: LocationHistoryMongoRepository,
    private val objectMapper: ObjectMapper
) {

    suspend fun saveLocationFromMqtt(mqttResponse: MqttResponse) {
        val entity = LocationHistory(
            userId = mqttResponse.userId,
            lat = mqttResponse.lat,
            lng = mqttResponse.lng,
            timestamp = mqttResponse.timestamp
        )
        locationHistoryMongoRepository.save(entity).awaitFirstOrNull()
    }

    suspend fun saveLocationAsyncFromJson(json: String) {
        val entity = parseJsonToEntity(json)
        locationHistoryMongoRepository.save(entity).awaitFirstOrNull()
    }

    private fun parseJsonToEntity(json: String): LocationHistory {
        return try {
            objectMapper.readValue(json, LocationHistory::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid location payload: $json", e)
        }
    }

    suspend fun findAllByUserId(userId: String) : List<LocationHistory> =
        locationHistoryMongoRepository.findAllByUserId(userId).collectList().awaitFirst()




    // ================== ReactiveMongoTemplate ==================
    suspend fun saveLocationAsync(payload: ByteArray) {
        val location = parsePayload(payload) // JSON 역직렬화 등
        mongoTemplate.save(location).awaitFirstOrNull()
    }

    private fun parsePayload(payload: ByteArray): Location {
        return try {
            objectMapper.readValue(payload)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid payload: ${payload.decodeToString()}", e)
        }
    }
}
