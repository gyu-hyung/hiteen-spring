package kr.jiasoft.hiteen.feature.interest.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.interest.domain.InterestEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestRegisterRequest
import kr.jiasoft.hiteen.feature.interest.dto.InterestResponse
import kr.jiasoft.hiteen.feature.interest.dto.toResponse
import kr.jiasoft.hiteen.feature.interest.infra.InterestRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class InterestService(
    private val interestRepository: InterestRepository
) {
    suspend fun createInterest(
        user: UserEntity,
        interest: InterestRegisterRequest
    ): InterestEntity {
        return interestRepository.save(
            InterestEntity(
                topic = interest.topic,
                category = interest.category,
                status = "Y",
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )
    }

    suspend fun getInterest(id: Long): InterestEntity? {
        return interestRepository.findById(id)
    }


    suspend fun getAllInterestsByUser(userId: Long): Map<String, List<InterestResponse>> {
        val all = interestRepository.findAllWithUserStatus(userId).toList()
        return all.groupBy { it.category }
            .mapValues { (_, list) -> list.map { it.toResponse() }.sortedBy { e -> e.id } }
    }


    suspend fun updateInterest(
        user: UserEntity, updated: InterestRegisterRequest
    ): InterestEntity? {
        val exist = interestRepository.findById(updated.id) ?: return null

        val merged = exist.copy(
            id = updated.id,
            topic = updated.topic.takeIf { it.isNotBlank() } ?: exist.topic,
            category = updated.category.takeIf { it.isNotBlank() } ?: exist.category,
            status = updated.status.takeIf { it.isNotBlank() } ?: exist.status,
            updatedId = user.id,
            updatedAt = OffsetDateTime.now()
        )
        return interestRepository.save(merged)
    }


    suspend fun deleteInterest(id: Long, deletedId: Long): Boolean {
        val exist = interestRepository.findById(id) ?: return false
        val deleted = exist.copy(
            deletedId = deletedId,
            deletedAt = java.time.OffsetDateTime.now()
        )
        interestRepository.save(deleted)
        return true
    }
}
