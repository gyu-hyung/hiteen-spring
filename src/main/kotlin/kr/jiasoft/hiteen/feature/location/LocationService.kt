package kr.jiasoft.hiteen.feature.location

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import kotlinx.coroutines.reactive.awaitFirst
import kr.jiasoft.hiteen.feature.mqtt.MqttResponse

@Service
class LocationService(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val locationHistoryMongoRepository: LocationHistoryMongoRepository,
    private val objectMapper: ObjectMapper,
) {

    //HTTP로 위치정보 이력 저장
    suspend fun saveLocation(entity: LocationHistory): LocationHistory? {
        return locationHistoryMongoRepository.save(entity).awaitFirstOrNull()
    }

    //MQTT로 위치정보 이력 저장
    suspend fun saveLocationFromMqtt(mqttResponse: MqttResponse) {
        val entity = LocationHistory(
            userId = mqttResponse.userId,
            lat = mqttResponse.lat,
            lng = mqttResponse.lng,
            timestamp = mqttResponse.timestamp
        )
        locationHistoryMongoRepository.save(entity).awaitFirstOrNull()
    }


    suspend fun findAllByUserId(userId: String) : List<LocationHistory> =
        locationHistoryMongoRepository.findAllByUserId(userId).collectList().awaitFirst()


}
