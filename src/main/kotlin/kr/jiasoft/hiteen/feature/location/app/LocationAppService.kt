package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.dto.LocationEvent
import kr.jiasoft.hiteen.feature.location.dto.LocationRequest
import kr.jiasoft.hiteen.feature.location.infra.realtime.LocationBroadcaster
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Service

@Service
class LocationAppService(
    private val locationService: LocationService,
    private val locationCacheRedisService: LocationCacheRedisService,
    private val broadcaster: LocationBroadcaster
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
            broadcaster.publishToUser(userUid, event)
            locationCacheRedisService.cacheLatest(saved)
        }
        return saved
    }

    suspend fun getMyLocations(user: UserEntity): List<LocationHistory> =
        locationService.findAllByUserId(user.username)
}
