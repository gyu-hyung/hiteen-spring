package kr.jiasoft.hiteen.feature.interest.infra

import kr.jiasoft.hiteen.feature.interest.domain.InterestUserEntity
import kr.jiasoft.hiteen.feature.interest.dto.InterestUserResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface InterestUserRepository : CoroutineCrudRepository<InterestUserEntity, Long> {

    suspend fun findByUserId(userId: Long): List<InterestUserEntity>

    suspend fun findByUserIdAndInterestId(userId: Long, interestId: Long): InterestUserEntity?

    suspend fun deleteByUserIdAndInterestId(userId: Long, interestId: Long): Long

    @Query("""
        SELECT
          (SELECT u.uid FROM users u WHERE u.id = iu.user_id) AS user_uid,
          i.id, i.topic, i.category, i.status,
          iu.*
        FROM interest_user iu
        JOIN interests i ON iu.interest_id = i.id
        WHERE (:id IS NULL OR iu.id = :id)
          AND (:userId IS NULL OR iu.user_id = :userId)
    """)
    suspend fun getInterestResponseById(id: Long?, userId: Long?): List<InterestUserResponse>


}
