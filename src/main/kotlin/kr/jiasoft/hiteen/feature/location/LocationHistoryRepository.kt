package kr.jiasoft.hiteen.feature.location

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationHistoryRepository : ReactiveMongoRepository<LocationHistory, String>