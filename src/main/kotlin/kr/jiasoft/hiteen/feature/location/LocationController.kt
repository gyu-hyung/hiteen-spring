package kr.jiasoft.hiteen.feature.location

import kr.jiasoft.hiteen.feature.user.UserEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/location")
class LocationController (
    private val locationService: LocationService,
    private val locationRedisService: LocationRedisService,
    private val publisher: LocationPublisher,
) {

    data class LocationRequest(
        val userId: String,
        val lat: Double,
        val lng: Double,
        val timestamp: Long
    )

    @GetMapping("/latest")
    suspend fun getLatest(username: String): LocationHistory? {
        return locationRedisService.getLatest(username)
    }

    @PostMapping
    suspend fun saveLocation(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody request: LocationRequest
    ): ResponseEntity<LocationHistory> {
        val entity = LocationHistory(
//            userId = user.username,
            userId = request.userId,
            lat = request.lat,
            lng = request.lng,
            timestamp = request.timestamp
        )
        val saved = locationService.saveLocation(entity)
        if (saved != null) {
            // 1) 브로드캐스트
            publisher.publishToUser(saved.userId, LocationEvent.from(saved))
            // 2) 최신 위치 캐시 갱신
            locationRedisService.cacheLatest(saved)
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @GetMapping("/my")
    suspend fun getLocation(@AuthenticationPrincipal(expression = "user") user: UserEntity): List<LocationHistory> {
        return locationService.findAllByUserId(user.username)
    }

}