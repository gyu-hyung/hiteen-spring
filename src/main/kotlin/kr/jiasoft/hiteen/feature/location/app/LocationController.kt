package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import kr.jiasoft.hiteen.feature.location.dto.LocationRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/location")
@Validated
class LocationController(
    private val locationAppService: LocationAppService
) {

    @PostMapping
    suspend fun saveLocation(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestBody request: LocationRequest
    ): ResponseEntity<LocationHistory> {
        val saved = locationAppService.saveLocation(user, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }


    @GetMapping("/latest")
    suspend fun getLatest(@RequestParam userId: String): LocationHistory? =
        locationAppService.getLatest(userId)


    @GetMapping("/my")
    suspend fun getMy(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): List<LocationHistory> =
        locationAppService.getMyLocations(user)

}