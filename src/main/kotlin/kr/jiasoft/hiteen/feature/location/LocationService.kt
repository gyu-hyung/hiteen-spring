package kr.jiasoft.hiteen.feature.location

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class LocationService(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val locationHistoryRepository: LocationHistoryRepository,
    private val objectMapper: ObjectMapper
) {
    suspend fun saveLocationAsyncFromJson(json: String) {
        val entity = parseJsonToEntity(json)
        locationHistoryRepository.save(entity).awaitFirstOrNull()
    }

    private fun parseJsonToEntity(json: String): LocationHistory {
        return try {
            objectMapper.readValue(json, LocationHistory::class.java)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid location payload: $json", e)
        }
    }



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
