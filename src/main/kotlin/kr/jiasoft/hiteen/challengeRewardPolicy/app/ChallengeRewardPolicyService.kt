package kr.jiasoft.hiteen.challengeRewardPolicy.app

import kr.jiasoft.hiteen.challengeRewardPolicy.domain.ChallengeRewardPolicyEntity
import kr.jiasoft.hiteen.challengeRewardPolicy.infra.ChallengeRewardPolicyRepository
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicyDeleteRequest
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicyRow
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicySaveRequest
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class ChallengeRewardPolicyService(
    private val repository: ChallengeRewardPolicyRepository,
) {

    /** 목록 조회 */
    suspend fun listByPage(
        page: Int,
        size: Int,
//        order: String,
        search: String?,
        searchType: String,
        status: String?,
    ): List<ChallengeRewardPolicyRow> =
        repository.listByPage(
            page = page,
            size = size,
//            order = order,
            search = search,
            searchType = searchType,
            status = status,
        ).toList()

    /** 개수조회 */
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
    ): Int =
        repository.totalCount(
            search = search,
            searchType = searchType,
            status = status,
        )

    /** 등록/수정/순서 저장 (벌크) */
    suspend fun saveAll(
        req: ChallengeRewardPolicySaveRequest,
        adminId: Long
    ) {
        req.items.forEach { item ->
            val entity = ChallengeRewardPolicyEntity(
                id = item.id ?: 0,
                type = item.type,
                league = item.league,
                gameId = item.gameId,
                amount = item.amount,
                goodsCodes = item.goodsCodes,
                rank = item.rank,
                message = item.message,
                memo = item.memo,
                status = item.status,
                orderNo = item.orderNo,
                assetUid = item.assetUid,
                createdId = adminId,
                createdAt = OffsetDateTime.now(),
                updatedId = adminId,
                updatedAt = if(item.id != null) OffsetDateTime.now() else null,
            )
            repository.save(entity)
        }
    }

    /** 삭제 (소프트 삭제) */
    suspend fun delete(
        req: ChallengeRewardPolicyDeleteRequest,
        adminId: Long
    ) {
        req.ids.forEach { id ->
            repository.findById(id)?.let {
                repository.save(
                    it.copy(
                        deletedId = adminId,
                        deletedAt = OffsetDateTime.now()
                    )
                )
            }
        }
    }
}
