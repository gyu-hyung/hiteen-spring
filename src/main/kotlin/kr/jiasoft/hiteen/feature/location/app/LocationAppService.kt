package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.dto.LocationEvent
import kr.jiasoft.hiteen.feature.location.dto.LocationRequest
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
import kr.jiasoft.hiteen.feature.soketi.app.SoketiBroadcaster
import kr.jiasoft.hiteen.feature.soketi.domain.SoketiChannelPattern
import kr.jiasoft.hiteen.feature.soketi.domain.SoketiEventType
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Service

@Service
class LocationAppService(
    private val locationService: LocationService,
    private val locationCacheRedisService: LocationCacheRedisService,
    private val soketiBroadcaster: SoketiBroadcaster,
) {

    suspend fun getLatest(userId: String): LocationHistory? =
        locationCacheRedisService.getLatest(userId)

    suspend fun saveLocation(
        user: UserEntity,
        req: LocationRequest
    ): LocationHistory? {
        val userUid = user.uid.toString()

        val entity = LocationHistory(
            userId = userUid,
            lat = req.lat,
            lng = req.lng,
            timestamp = req.timestamp
        )

        val saved = locationService.saveLocation(entity)
        if (saved != null) {
            val event = LocationEvent(
                userId = userUid,
                lat = req.lat,
                lng = req.lng,
                timestamp = req.timestamp,
                source = "http"
            )

            val payload = mapOf(
                "userId" to userUid,
                "lat" to event.lat,
                "lng" to event.lng,
                "timestamp" to event.timestamp.toString(),
                "source" to event.source
            )

            soketiBroadcaster.broadcast(
                SoketiChannelPattern.PRIVATE_USER_LOCATION.format(userUid),
                SoketiEventType.LOCATION,
                payload
            )
            locationCacheRedisService.cacheLatest(saved)
        }

        return saved
    }

    suspend fun getMyLocations(user: UserEntity): List<LocationHistory> =
        locationService.findAllByUserId(user.uid.toString())
}
