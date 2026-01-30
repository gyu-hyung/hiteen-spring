package kr.jiasoft.hiteen.challengeRewardPolicy.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.challengeRewardPolicy.domain.ChallengeRewardPolicyEntity
import kr.jiasoft.hiteen.challengeRewardPolicy.dto.ChallengeRewardPolicyRow
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

            AND (
                :type IS NULL OR :type = 'ALL'
                OR crp.type = :type
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        type: String?,
    ): Int


    /**
     * 🔹 페이징 목록 조회
     */
    @Query("""
        SELECT 
            crp.*,
            (SELECT name FROM games WHERE id = crp.game_id) AS game_display_name 
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

            AND (
                :type IS NULL OR :type = 'ALL'
                OR crp.type = :type
            )

        ORDER BY
            (select min(level) from tiers where tier_code LIKE CONCAT('%', league, '%')),
            crp.order_no ASC,
            crp.rank ASC,
            crp.id DESC

        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
//        order: String,      // (확장 대비용, 현재는 order_no 기준)
        search: String?,
        searchType: String,
        status: String?,
        type: String?,
    ): Flow<ChallengeRewardPolicyRow>
}
