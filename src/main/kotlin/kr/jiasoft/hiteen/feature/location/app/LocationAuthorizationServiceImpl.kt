package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.feature.location.domain.LocationAuthorizationService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LocationAuthorizationServiceImpl : LocationAuthorizationService {
    override fun assertCanSubscribe(requesterId: Long, targetUserIds: List<UUID>) {
        // TODO: 친구만/본인, 안개모드?
    }
}