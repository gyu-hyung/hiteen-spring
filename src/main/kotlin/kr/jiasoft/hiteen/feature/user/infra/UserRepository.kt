package kr.jiasoft.hiteen.feature.user.infra

import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.PublicUser
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface UserRepository : CoroutineCrudRepository<UserEntity, Long> {
    suspend fun findByUsername(name: String): UserEntity?
    suspend fun findByUid(uid: String): UserEntity?

    /* 간단 검색: username/nickname/email 에 like (필요시 정규화/풀텍스트로 교체) */
    @Query("""
        SELECT uid, username, nickname, telno, address, detail_address, mood, tier, asset_uid
        FROM users
        WHERE (LOWER(username) LIKE LOWER(CONCAT('%', :q, '%'))
           OR  LOWER(COALESCE(nickname,'')) LIKE LOWER(CONCAT('%', :q, '%'))
           OR  LOWER(COALESCE(email,'')) LIKE LOWER(CONCAT('%', :q, '%')))
        LIMIT :limit
    """)
    suspend fun searchPublic(q: String, limit: Int = 30): List<PublicUser>

    /* 친구/팔로우 사용자 정보회 */
    @Query("""
        SELECT uid, username, nickname, telno, address, detail_address, mood, tier, asset_uid 
        FROM users WHERE id = :id
    """)
    suspend fun findPublicById(id: Long): PublicUser?

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
}