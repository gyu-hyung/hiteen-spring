package kr.jiasoft.hiteen.feature.user.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : CoroutineCrudRepository<UserEntity, Long> {
    @Cacheable(cacheNames = ["userEntityTest"], key = "#name")
    suspend fun findByUsername(name: String): UserEntity?
    suspend fun findByNickname(name: String): UserEntity?
    suspend fun findByPhone(phone: String): UserEntity?
    suspend fun findByUid(uid: String): UserEntity?
    suspend fun findAllByNickname(nickname: String): Flow<UserEntity>
    suspend fun findAllByPhone(phone: String): Flow<UserEntity>
    suspend fun findByInviteCode(inviteCode: String): UserEntity?


    // ✅ 활성(미삭제) 사용자 - username(=phone)로 조회
    @Query("""SELECT * FROM users WHERE username = :username AND deleted_at IS NULL ORDER BY id DESC LIMIT 1""")
    suspend fun findActiveByUsername(username: String): UserEntity?

    // ✅ 활성 사용자 - phone으로 조회 (username과 동일하지만, 명시적 함수)
    @Query("""SELECT * FROM users WHERE phone = :phone AND deleted_at IS NULL LIMIT 1""")
    suspend fun findActiveByPhone(phone: String): UserEntity?

    // ✅ 탈퇴(삭제)된 사용자 중 가장 최근
    @Query("""SELECT * FROM users WHERE phone = :phone AND deleted_at IS NOT NULL ORDER BY deleted_at DESC LIMIT 1""")
    suspend fun findLatestDeletedByPhone(phone: String): UserEntity?

    // ✅ 닉네임 활성 중복 체크
    @Query("""SELECT EXISTS(SELECT 1 FROM users WHERE nickname = :nickname AND deleted_at IS NULL)""")
    suspend fun existsByNicknameActive(nickname: String): Boolean

    // ✅ email 활성 중복 체크(선택)
    @Query("""SELECT EXISTS(SELECT 1 FROM users WHERE lower(email) = lower(:email) AND deleted_at IS NULL)""")
    suspend fun existsByEmailActive(email: String): Boolean


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


    @Query("""
        SELECT 
            u.id AS id,
            u.uid AS uid,
            u.username AS username,
            u.nickname AS nickname,
            u.address AS address,
            u.detail_address AS detail_address,
            u.phone AS phone,
            u.mood AS mood,
            u.mood_emoji AS mood_emoji,
            u.mbti AS mbti,
            u.exp_points AS exp_points,
            t.id AS tier_id,
            t.tier_name_kr AS tier_name,
            u.asset_uid AS asset_uid,
            CASE 
                WHEN f.id IS NOT NULL THEN true 
                ELSE false 
            END AS is_friend,
            CASE 
                WHEN fr.id IS NOT NULL THEN true 
                ELSE false 
            END AS is_friend_request
        FROM users u
        LEFT JOIN tiers t ON u.tier_id = t.id
        LEFT JOIN friends f 
            ON ((f.user_id = :currentUserId AND f.friend_id = u.id) 
             OR (f.friend_id = :currentUserId AND f.user_id = u.id))
            AND f.status = 'ACCEPTED'
        LEFT JOIN friends fr 
            ON fr.user_id = :currentUserId 
            AND fr.friend_id = u.id 
            AND fr.status = 'PENDING'
        WHERE u.phone IN (:phones)
    """)
    fun findAllUserSummaryByPhoneIn(
        phones: Set<String>,
        currentUserId: Long
    ): Flow<UserSummary>



    suspend fun findIdByUidIn(uids: List<UUID>): List<Long>


}