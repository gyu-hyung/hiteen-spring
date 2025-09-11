package kr.jiasoft.hiteen.feature.user.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserPhotosRepository : CoroutineCrudRepository<UserPhotosEntity, Long> {

    suspend fun findByIdAndUserId(id: Long, userId: Long): UserPhotosEntity?
    suspend fun findByUserId(userId: Long): Flow<UserPhotosEntity>
    suspend fun deleteByIdAndUserId(id: Long, userId: Long): Long

}