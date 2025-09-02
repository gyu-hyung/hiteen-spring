package kr.jiasoft.hiteen.feature.location.app

import kr.jiasoft.hiteen.feature.location.domain.LocationAuthorizationService
import org.springframework.stereotype.Service

@Service
class LocationAuthorizationServiceImpl : LocationAuthorizationService {
    override fun assertCanSubscribe(requesterId: Long, targetUserIds: List<String>) {
        // TODO: 친구만/본인, 안개모드?
    }
}