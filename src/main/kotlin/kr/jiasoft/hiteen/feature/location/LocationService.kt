package kr.jiasoft.hiteen.feature.location

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import com.fasterxml.jackson.module.kotlin.readValue

@Service
class LocationService(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val objectMapper: ObjectMapper
) {
    suspend fun saveLocationAsync(payload: ByteArray) {
        val location = parsePayload(payload) // JSON 역직렬화 등
        mongoTemplate.save(location).awaitFirstOrNull()
    }

    private fun parsePayload(payload: ByteArray): Location {
        return try {
            objectMapper.readValue(payload)
        } catch (e: Exception) {
            // 로깅 또는 기본값/에러 throw 등 처리
            throw IllegalArgumentException("Invalid payload: ${payload.decodeToString()}", e)
        }
    }
}
