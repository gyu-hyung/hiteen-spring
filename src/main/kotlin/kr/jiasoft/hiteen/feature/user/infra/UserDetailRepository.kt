package kr.jiasoft.hiteen.feature.user.infra

import kr.jiasoft.hiteen.feature.user.domain.UserDetailEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserDetailRepository : CoroutineCrudRepository<UserDetailEntity, Long> {

    suspend fun findByUserId(userId: Long): UserDetailEntity?

    //uid로 조회
    @Query("SELECT * FROM user_details WHERE user_id = (select id from users where uid = :uid ) LIMIT 1")
    suspend fun findByUid(uid: UUID): UserDetailEntity?

    //uid로 삭제
    @Query("DELETE FROM user_details WHERE user_id = (select id from users where uid = :uid )")
    suspend fun deleteByUid(uid: UUID): Int

    @Query("DELETE FROM user_details WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Long): Int
}
