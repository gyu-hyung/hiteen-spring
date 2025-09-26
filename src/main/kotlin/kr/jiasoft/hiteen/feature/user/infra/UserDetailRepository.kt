package kr.jiasoft.hiteen.feature.user.infra

import kr.jiasoft.hiteen.feature.user.domain.UserDetailEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserDetailRepository : CoroutineCrudRepository<UserDetailEntity, Long> {

    suspend fun findByUserId(userId: Long): UserDetailEntity?

    @Query("DELETE FROM user_details WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long): Int
}
