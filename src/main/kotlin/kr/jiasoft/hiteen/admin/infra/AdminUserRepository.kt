package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSearchResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AdminUserRepository : CoroutineCrudRepository<UserEntity, Long> {

    suspend fun findByUid(uid: UUID): UserEntity?

    @Query("""
        SELECT COUNT(*)
        FROM users u
        LEFT JOIN schools s ON s.id = u.school_id
        LEFT JOIN tiers t ON t.id = u.tier_id
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND u.deleted_at IS NULL)
                OR (:status = 'BLOCKED' AND u.deleted_at IS NOT NULL)
                OR (:status = 'DELETED' AND u.deleted_at IS NOT NULL)
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR u.role = :type
            )
            AND (
                :startDate IS NULL
                OR u.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR u.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (
                            u.nickname ILIKE ('%' || :search || '%')
                            OR u.phone ILIKE ('%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%')
                            OR s.name ILIKE ('%' || :search || '%')
                            OR t.tier_name_kr ILIKE ('%' || :search || '%')
                        )
                    WHEN :searchType = 'NICKNAME' THEN
                        u.nickname ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'PHONE' THEN
                        u.phone ILIKE ('%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%')
                    WHEN :searchType = 'SCHOOL' THEN
                        s.name ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'TIER' THEN
                        t.tier_name_kr ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
    """)
    suspend fun totalCount(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
    ): Int

    @Query("""
        SELECT 
            u.*,
            s.name AS school_name,
            t.tier_name_kr,
            t.uid AS tier_asset_uid,
            t.level,
            ups.total_point AS point,
            ucs.total_cash AS cash
        FROM users u
        LEFT JOIN schools s ON s.id = u.school_id
        LEFT JOIN tiers t ON t.id = u.tier_id
        LEFT JOIN user_points_summary ups ON ups.user_id = u.id
        LEFT JOIN user_cash_summary ucs ON ucs.user_id = u.id
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND u.deleted_at IS NULL)
                OR (:status = 'BLOCKED' AND u.deleted_at IS NOT NULL)
                OR (:status = 'DELETED' AND u.deleted_at IS NOT NULL)
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR u.role = :type
            )
            AND (
                :startDate IS NULL
                OR u.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR u.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (
                            u.nickname ILIKE ('%' || :search || '%')
                            OR u.phone ILIKE ('%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%')
                            OR s.name ILIKE ('%' || :search || '%')
                            OR t.tier_name_kr ILIKE ('%' || :search || '%')
                        )
                    WHEN :searchType = 'NICKNAME' THEN
                        u.nickname ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'PHONE' THEN
                        u.phone ILIKE ('%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%')
                    WHEN :searchType = 'SCHOOL' THEN
                        s.name ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'TIER' THEN
                        t.tier_name_kr ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
        ORDER BY 
            CASE WHEN :order = 'DESC' THEN u.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN u.created_at END ASC
        LIMIT :size OFFSET :offset
    """)
    fun listByPage(
        status: String?,
        type: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        order: String?,
        size: Int,
        offset: Int,
    ): Flow<AdminUserResponse>

    @Query("""
        SELECT 
            u.*,
            s.name AS school_name,
            t.tier_name_kr,
            t.uid AS tier_asset_uid,
            t.level,
            ups.total_point AS point,
            ucs.total_cash AS cash
        FROM users u
        LEFT JOIN schools s ON s.id = u.school_id
        LEFT JOIN tiers t ON t.id = u.tier_id
        LEFT JOIN user_points_summary ups ON ups.user_id = u.id
        LEFT JOIN user_cash_summary ucs ON ucs.user_id = u.id
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
            AND u.role = 'USER'
            AND (
                :keyword IS NULL
                OR u.nickname ILIKE ('%' || :keyword || '%')
                OR u.phone ILIKE ('%' || regexp_replace(:keyword, '[^0-9]', '', 'g') || '%')
            )
    """)
    suspend fun listSearchUsers(
         keyword: String?
    ): Flow<AdminUserSearchResponse>

    /**
     * 회원 검색(ROLE 조건 없음) - AdminUserController 전용
     * 기존 listSearchUsers는 다른 곳에서 사용할 수 있으므로 건드리지 않고 신규로 추가.
     */
    @Query("""
        SELECT u.*
        FROM users u
        WHERE u.deleted_at IS NULL
            AND (
                :keyword IS NULL
                OR u.nickname ILIKE ('%' || :keyword || '%')
                OR u.phone ILIKE ('%' || regexp_replace(:keyword, '[^0-9]', '', 'g') || '%')
            )
    """)
    suspend fun listSearchUsersAllRoles(
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
    suspend fun findUsersByPhonesAndRole(
        role: String? = "USER",
        phones: List<String>
    ): List<UserEntity>


    // 휴대폰번호 기준 회원 목록
    @Query("""
        SELECT *
        FROM users u
        WHERE u.phone IS NOT NULL
          AND u.phone IN (:phones)
    """)
    suspend fun findUsersByPhones(
        phones: List<String>
    ): List<UserEntity>

    @Query("""
        SELECT 
            u.*,
            s.name AS school_name,
            t.tier_name_kr,
            t.uid AS tier_asset_uid,
            t.level,
            ups.total_point AS point,
            ucs.total_cash AS cash
        FROM users u
        LEFT JOIN schools s ON s.id = u.school_id
        LEFT JOIN tiers t ON t.id = u.tier_id
        LEFT JOIN user_points_summary ups ON ups.user_id = u.id
        LEFT JOIN user_cash_summary ucs ON ucs.user_id = u.id
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND u.deleted_at IS NULL)
                OR (:status = 'BLOCKED' AND u.deleted_at IS NOT NULL)
                OR (:status = 'DELETED' AND u.deleted_at IS NOT NULL)
            )
            AND (:role IS NULL OR u.role = :role)
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (u.nickname ILIKE ('%' || :search || '%')
                         OR u.phone ILIKE ('%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%'))
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'phone' THEN
                        u.phone ILIKE ('%' || regexp_replace(:search, '[^0-9]', '', 'g') || '%')
                    ELSE TRUE
                END
            )
            AND (:lastId IS NULL OR u.id < :lastId)
        ORDER BY u.id DESC
        LIMIT :size
    """)
    fun listByCursorId(
        size: Int,
        lastId: Long?,
        search: String?,
        searchType: String,
        role: String?,
        status: String?,
    ): Flow<AdminUserResponse>
}