package kr.jiasoft.hiteen.feature.location.app

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.feature.integration.mqtt.dto.MqttResponse
import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.infra.db.LocationHistoryMongoRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LocationService(
    private val locationHistoryMongoRepository: LocationHistoryMongoRepository,
) {
    // HTTP로 위치정보 이력 저장
    suspend fun saveLocation(entity: LocationHistory): LocationHistory? =
        locationHistoryMongoRepository.save(entity).awaitFirstOrNull()

    // MQTT로 위치정보 이력 저장
    suspend fun saveLocationFromMqtt(m: MqttResponse) {
        val entity = LocationHistory(
            userId = m.userId,
            lat = m.lat,
            lng = m.lng,
            timestamp = m.timestamp
        )
        locationHistoryMongoRepository.save(entity).awaitFirstOrNull()
    }

    suspend fun findAllByUserId(userUid: String): List<LocationHistory> =
        locationHistoryMongoRepository.findAllByUserId(userUid).collectList().awaitFirst()
}
