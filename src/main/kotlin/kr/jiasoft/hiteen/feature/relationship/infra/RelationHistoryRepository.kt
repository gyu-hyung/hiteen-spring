package kr.jiasoft.hiteen.feature.relationship.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.relationship.domain.RelationEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface RelationHistoryRepository : CoroutineCrudRepository<RelationEntity, Long> {

    fun findAllByUserIdAndTargetId(userId: Long, targetId: Long): Flow<RelationEntity>?

    suspend fun findAllByUserIdAndTargetIdAndRelationTypeAndAction(userId: Long, targetId: Long, relationType: String, action: String): Flow<RelationEntity>?

    suspend fun existsByUserIdAndTargetIdAndRelationTypeAndAction(
        userId: Long,
        targetId: Long,
        relationType: String,
        action: String,
    ): Boolean
}
