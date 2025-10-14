package kr.jiasoft.hiteen.feature.user.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface UserRepository : CoroutineCrudRepository<UserEntity, Long> {
    suspend fun findByUsername(name: String): UserEntity?
    suspend fun findByNickname(name: String): UserEntity?
    suspend fun findByPhone(phone: String): UserEntity?
    suspend fun findByUid(uid: String): UserEntity?
    suspend fun findAllByNickname(nickname: String): Flow<UserEntity>
    suspend fun findAllByPhone(phone: String): Flow<UserEntity>
    suspend fun findByInviteCode(inviteCode: String): UserEntity?

    @Query("""
        UPDATE users SET mbti = :mbti WHERE id = :userId
    """)
    @Modifying
    suspend fun updateMbti(userId: Long, mbti: String)

    /* 간단 검색: username/nickname/email 에 like (필요시 정규화/풀텍스트로 교체) */
    @Query("""
        SELECT a.*
        , (select tier_name_kr from tiers where id = tier_id) tier_name
        FROM users a
        WHERE (LOWER(username) LIKE LOWER(CONCAT('%', :q, '%'))
           OR  LOWER(COALESCE(nickname,'')) LIKE LOWER(CONCAT('%', :q, '%'))
           OR  LOWER(COALESCE(email,'')) LIKE LOWER(CONCAT('%', :q, '%')))
        LIMIT :limit
    """)
    suspend fun searchSummary(q: String, limit: Int = 30): Flow<UserSummary>

    /* 친구/팔로우 사용자 정보회 */
    @Query("""
        SELECT a.*
        , (select tier_name_kr from tiers where id = tier_id) tier_name
        FROM users a WHERE id = :id
    """)
    suspend fun findSummaryInfoById(id: Long): UserSummary

    
    @Query("""
        SELECT a.*
        , (select tier_name_kr from tiers where id = tier_id) tier_name
        FROM users a WHERE id IN (:ids)
    """)
    suspend fun findSummaryByIds(ids: List<Long>): List<UserSummary>


    @Modifying
    @Query("""
        UPDATE users 
        SET exp_points = :exp, tier_id = :tierId 
        WHERE id = :userId
    """)
    suspend fun updateExpAndTier(userId: Long, exp: Long, tierId: Long?)

    @Query("""
        SELECT EXISTS (
          SELECT 1
          FROM users
          WHERE deleted_at IS NULL
            AND lower(email) = lower(:email)
            AND id <> :excludeId
        )
    """)
    suspend fun existsByEmailIgnoreCaseAndActiveAndIdNot(email: String, excludeId: Long): Boolean


    @Query("""
        SELECT EXISTS (
          SELECT 1
          FROM users
          WHERE deleted_at IS NULL
            AND lower(phone) = lower(:phone)
            AND id <> :excludeId
        )
    """)
    suspend fun existsByPhoneAndActiveAndIdNot(phone: String, excludeId: Long): Boolean

    @Query("""
        SELECT EXISTS (
          SELECT 1
          FROM users
          WHERE deleted_at IS NULL
            AND lower(username) = lower(:username)
            AND id <> :excludeId
        )
    """)
    suspend fun existsByUsernameIgnoreCaseAndActiveAndIdNot(username: String, excludeId: Long): Boolean

    @Query(
        """
        SELECT id
        FROM users
        WHERE deleted_at IS NULL
          AND lower(username) = lower(:username)
        LIMIT 1
        """
    )
    suspend fun findIdByUsername(username: String): Long?

    @Query("""
        SELECT uid
        FROM users
        WHERE deleted_at IS NULL
          AND lower(username) = lower(:username)
        LIMIT 1
        """)
    suspend fun findUidByUsername(username: String): UUID?

    @Query("""
        SELECT uid
        FROM users
        WHERE deleted_at IS NULL
          AND id = :id
        LIMIT 1
        """)
    suspend fun findUidById(id: Long): UUID?

    @Query("""
        SELECT id
        FROM users
        WHERE deleted_at IS NULL
          AND uid = :uid
        LIMIT 1
        """)
    suspend fun findIdByUid(uid: UUID): Long?


    suspend fun findAllByPhoneIn(phones: Set<String>): Flow<UserEntity>

    suspend fun findAllByUidIn(uids: List<UUID>): List<UserEntity>
    suspend fun findIdByUidIn(uids: List<UUID>): List<Long>


}