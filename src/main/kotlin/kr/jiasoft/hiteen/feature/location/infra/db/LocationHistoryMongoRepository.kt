package kr.jiasoft.hiteen.feature.location.infra.db

import kr.jiasoft.hiteen.feature.location.domain.LocationHistory
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
interface LocationHistoryMongoRepository : ReactiveMongoRepository<LocationHistory, String> {
    fun findAllByUserId(userId: String): Flux<LocationHistory>
    fun findTopByUserIdOrderByTimestampDesc(userId: String): Mono<LocationHistory>
}