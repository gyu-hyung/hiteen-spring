package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.infra.messaging.LocationEvent
import kr.jiasoft.hiteen.feature.location.infra.messaging.LocationBroadcaster
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationRedisService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Service

@Service
class LocationAppService(
    private val locationService: LocationService,
    private val locationRedisService: LocationRedisService,
    private val broadcaster: LocationBroadcaster
) {

    suspend fun getLatest(userId: String): LocationHistory? =
        locationRedisService.getLatest(userId)

    suspend fun saveLocation(
        user: UserEntity,
        req: LocationController.LocationRequest
    ): LocationHistory? {
        val entity = LocationHistory(
            //TODO uid? or id?
//            userId = user.uid.toString(),
            userId = user.id.toString(),
            lat = req.lat,
            lng = req.lng,
            timestamp = req.timestamp
        )
        val saved = locationService.saveLocation(entity)
        if (saved != null) {
            broadcaster.publishToUser(saved.userId, LocationEvent.from(saved))
            locationRedisService.cacheLatest(saved)
        }
        return saved
    }

    suspend fun getMyLocations(user: UserEntity): List<LocationHistory> =
        locationService.findAllByUserId(user.username)
}
