package kr.jiasoft.hiteen.feature.relationship

import kr.jiasoft.hiteen.feature.relationship.domain.RelationEntity
import kr.jiasoft.hiteen.feature.relationship.infra.RelationHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RelationHistoryService (
    private val relationHistoryRepository: RelationHistoryRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)


    suspend fun log(userId: Long, targetId: Long, relationType: String, relationAction: String) {
        try {
            relationHistoryRepository.save(
                RelationEntity(
                    userId = userId,
                    targetId = targetId,
                    relationType = relationType,
                    action = relationAction,
                )
            )
        } catch (e: Exception) {
            log.info(e.message)
        }
    }

}