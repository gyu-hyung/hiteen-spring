package kr.jiasoft.hiteen.admin.feature.user

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.feature.user.dto.AdminUserResponse
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdminUserRepository : CoroutineCrudRepository<UserEntity, Long> {


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
            AND (:role IS NULL OR u.role = :role)
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND u.deleted_at IS NULL)
                OR (:status = 'DELETED' AND u.deleted_at IS NOT NULL)
            )
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
            (SELECT level FROM tiers t WHERE t.id = u.tier_id) AS level
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
            AND (:role IS NULL OR u.role = :role)
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND u.deleted_at IS NULL)
                OR (:status = 'DELETED' AND u.deleted_at IS NOT NULL)
            )
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
            (SELECT level FROM tiers t WHERE t.id = u.tier_id) AS level
        FROM users u
        WHERE u.uid = :uid
    """)
    suspend fun findByUid(uid: UUID): AdminUserResponse?








}