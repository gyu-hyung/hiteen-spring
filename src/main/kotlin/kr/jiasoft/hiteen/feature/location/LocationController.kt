package kr.jiasoft.hiteen.feature.location

import kr.jiasoft.hiteen.feature.user.UserEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/location")
class LocationController (
    private val locationService: LocationService
) {

    @GetMapping("/my")
    suspend fun getLocation(@AuthenticationPrincipal(expression = "user") user: UserEntity): List<LocationHistory> {
        return locationService.findAllByUserId(user.username)
    }

}