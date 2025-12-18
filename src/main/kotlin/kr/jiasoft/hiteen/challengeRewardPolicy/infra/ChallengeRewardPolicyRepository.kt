package kr.jiasoft.hiteen.challengeRewardPolicy.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.challengeRewardPolicy.domain.ChallengeRewardPolicyEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ChallengeRewardPolicyRepository
    : CoroutineCrudRepository<ChallengeRewardPolicyEntity, Long> {

    /**
     * 🔹 전체 개수 조회
     */
    @Query("""
        SELECT COUNT(*)
        FROM challenge_reward_policy crp
        WHERE
            crp.deleted_at IS NULL

            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        crp.message ILIKE CONCAT('%', :search, '%')
                        OR crp.memo ILIKE CONCAT('%', :search, '%')
                        OR crp.league ILIKE CONCAT('%', :search, '%')
                        OR crp.type ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'message' AND crp.message ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'memo' AND crp.memo ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'league' AND crp.league ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'type' AND crp.type ILIKE CONCAT('%', :search, '%'))
            )

            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND crp.status = 1)
                OR (:status = 'INACTIVE' AND crp.status = 0)
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
    ): Int


    /**
     * 🔹 페이징 목록 조회
     */
    @Query("""
        SELECT crp.*
        FROM challenge_reward_policy crp
        WHERE
            crp.deleted_at IS NULL

            AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        crp.message ILIKE CONCAT('%', :search, '%')
                        OR crp.memo ILIKE CONCAT('%', :search, '%')
                        OR crp.league ILIKE CONCAT('%', :search, '%')
                        OR crp.type ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'message' AND crp.message ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'memo' AND crp.memo ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'league' AND crp.league ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'type' AND crp.type ILIKE CONCAT('%', :search, '%'))
            )

            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND crp.status = 1)
                OR (:status = 'INACTIVE' AND crp.status = 0)
            )

        ORDER BY
            crp.order_no ASC,
            crp.rank ASC,
            crp.id ASC

        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
//        order: String,      // (확장 대비용, 현재는 order_no 기준)
        search: String?,
        searchType: String,
        status: String?,
    ): Flow<ChallengeRewardPolicyEntity>
}
