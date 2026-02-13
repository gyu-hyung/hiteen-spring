package kr.jiasoft.hiteen.feature.block.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.block.domain.UserBlockEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserBlockRepository : CoroutineCrudRepository<UserBlockEntity, Long> {

    /**
     * 특정 사용자가 차단한 사용자 목록 조회
     */
    fun findAllByUserId(userId: Long): Flow<UserBlockEntity>

    /**
     * 특정 사용자가 차단한 사용자 ID 목록 조회
     */
    @Query("""
        SELECT blocked_user_id 
        FROM user_blocks 
        WHERE user_id = :userId
    """)
    fun findBlockedUserIdsByUserId(userId: Long): Flow<Long>

    /**
     * 차단 관계 존재 여부 확인
     */
    suspend fun existsByUserIdAndBlockedUserId(userId: Long, blockedUserId: Long): Boolean

    /**
     * 특정 차단 관계 조회
     */
    suspend fun findByUserIdAndBlockedUserId(userId: Long, blockedUserId: Long): UserBlockEntity?

    /**
     * 차단 해제
     */
    suspend fun deleteByUserIdAndBlockedUserId(userId: Long, blockedUserId: Long): Long

    /**
     * 차단한 사용자 수 조회
     */
    suspend fun countByUserId(userId: Long): Long
}

