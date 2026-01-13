package kr.jiasoft.hiteen.feature.user.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface UserPhotosRepository : CoroutineCrudRepository<UserPhotosEntity, Long> {

    suspend fun findByIdAndUserId(id: Long, userId: Long): UserPhotosEntity?
    suspend fun findByUserId(userId: Long): Flow<UserPhotosEntity>?
    suspend fun deleteByIdAndUserId(id: Long, userId: Long): Long
    suspend fun countByUserId(userId: Long): Long

    // Admin 사용자 편집용
    suspend fun deleteByUserId(userId: Long): Long
    suspend fun findByUid(uid: UUID): UserPhotosEntity?

}