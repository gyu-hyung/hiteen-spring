package kr.jiasoft.hiteen.feature.timetable.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.timetable.domain.TimeUserEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface TimeUserRepository : CoroutineCrudRepository<TimeUserEntity, Long> {

    @Query("SELECT * FROM time_user WHERE user_id = :userId ORDER BY week, period")
    fun findAllByUserId(userId: Long): Flow<TimeUserEntity>

    @Query("SELECT * FROM time_user WHERE user_id = :userId AND week = :week AND period = :period")
    suspend fun findByUserAndSlot(userId: Long, week: Int, period: Int): TimeUserEntity?

    //전체삭제
    @Query("DELETE FROM time_user WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: Long)

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun findByUid(uid: String): UserEntity?
}
