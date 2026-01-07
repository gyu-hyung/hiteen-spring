package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSearchResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdminUserRepository : CoroutineCrudRepository<UserEntity, Long> {

    suspend fun findByUid(uid: UUID): UserEntity?

    @Query("""
        SELECT COUNT(*)
        FROM users u
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        u.nickname LIKE CONCAT('%', :search, '%')
                        OR u.phone LIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'nickname' AND u.nickname LIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'phone' AND u.phone LIKE CONCAT('%', :search, '%'))
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND u.deleted_at IS NULL)
                OR (:status = 'DELETED' AND u.deleted_at IS NOT NULL)
            )
            AND (:role IS NULL OR u.role = :role)
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        role: String?,
        status: String?,
    ): Int

    @Query("""
        SELECT 
            u.*,
            (SELECT name FROM schools s WHERE s.id = u.school_id) AS school_name,
            (SELECT tier_name_kr FROM tiers t WHERE t.id = u.tier_id) AS tier_name_kr,
            (SELECT uid FROM tiers t WHERE t.id = u.tier_id) AS tier_asset_uid,
            (SELECT level FROM tiers t WHERE t.id = u.tier_id) AS level,
            (select total_point from user_points_summary ups where ups.user_id = u.id) point,
            (select total_cash from user_cash_summary ups where ups.user_id = u.id) cash
        FROM users u
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        u.nickname LIKE CONCAT('%', :search, '%')
                        OR u.phone LIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'nickname' AND u.nickname LIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'phone' AND u.phone LIKE CONCAT('%', :search, '%'))
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND u.deleted_at IS NULL)
                OR (:status = 'DELETED' AND u.deleted_at IS NOT NULL)
            )
            AND (:role IS NULL OR u.role = :role)
        ORDER BY 
            CASE WHEN :order = 'DESC' THEN created_at END DESC,
            CASE WHEN :order = 'ASC' THEN created_at END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        role: String?,
        status: String?,
    ): Flow<AdminUserResponse>

    @Query("""
        SELECT 
            u.*,
            (SELECT name FROM schools s WHERE s.id = u.school_id) AS school_name,
            (SELECT tier_name_kr FROM tiers t WHERE t.id = u.tier_id) AS tier_name_kr,
            (SELECT uid FROM tiers t WHERE t.id = u.tier_id) AS tier_asset_uid,
            (SELECT level FROM tiers t WHERE t.id = u.tier_id) AS level,
            (select total_point from user_points_summary ups where ups.user_id = u.id) point,
            (select total_cash from user_cash_summary ups where ups.user_id = u.id) cash
        FROM users u
        WHERE u.uid = :uid
    """)
    suspend fun findResponseByUid(uid: UUID): AdminUserResponse?

    // 포인트 지급, 푸시 전송 등에서 호출
    // 총회원수
    suspend fun countByRoleAndDeletedAtIsNull(role: String? = "USER") : Long

    // 회원 검색
    @Query("""
        SELECT u.*
        FROM users u
        WHERE u.deleted_at IS NULL
            AND u.role = :role
            AND (
                :keyword IS NULL
                OR (
                        u.nickname LIKE CONCAT('%', :keyword, '%')
                        OR u.phone LIKE CONCAT('%', :keyword, '%')
                )
            )
    """)
    suspend fun listSearchUsers(
        role: String? = "USER",
        keyword: String?
    ): Flow<AdminUserSearchResponse>

    // 그룹별 회원 전체 목록
    suspend fun findByRole(role: String? = "USER"): List<UserEntity>

    // 휴대폰번호 기준 회원 목록
    @Query("""
        SELECT *
        FROM users u
        WHERE u.role = :role
          AND u.phone IS NOT NULL
          AND u.phone IN (:phones)
    """)
    suspend fun findUsersByPhones(
        role: String? = "USER",
        phones: List<String>
    ): List<UserEntity>
}