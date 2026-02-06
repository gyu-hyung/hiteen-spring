package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminFollowResponse
import kr.jiasoft.hiteen.feature.relationship.domain.FollowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminFollowRepository : CoroutineCrudRepository<FollowEntity, Long> {

    /**
     * 🔹 전체 개수 조회
     */
    @Query("""
        SELECT COUNT(*)
        FROM follows f
        JOIN users u_from ON u_from.id = f.user_id
        JOIN users u_to   ON u_to.id   = f.follow_id
        WHERE
            (
                :search IS NULL
                OR (
                    CASE
                        WHEN :searchType = 'ALL' THEN
                            u_from.nickname ILIKE '%' || :search || '%'
                            OR u_from.phone ILIKE '%' || :search || '%'
                            OR u_to.nickname ILIKE '%' || :search || '%'
                            OR u_to.phone ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'nickname' THEN
                            u_from.nickname ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'phone' THEN
                            u_from.phone ILIKE '%' || :search || '%'
                    END
                )
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR f.status = :status
            )
            AND (
                (:followType = 'FOLLOWING' AND u_from.uid = :uid)
                OR (:followType = 'FOLLOWERS' AND u_to.uid = :uid)
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        followType: String,
        status: String?,
        uid: UUID?,
    ): Int




    /**
     * 🔹 페이징 조회 (AdminFriendResponse)
     */
    @Query("""
        SELECT
            f.id                    AS follow_id,
    
            u_from.id               AS from_user_id,
            u_from.uid              AS from_user_uid,
            u_from.nickname         AS from_nickname,
            u_from.phone            AS from_phone,
    
            u_to.id                 AS to_user_id,
            u_to.uid                AS to_user_uid,
            u_to.nickname           AS to_nickname,
            u_to.phone              AS to_phone,
    
            f.status,
            f.status_at,
            f.created_at
        FROM follows f
        JOIN users u_from ON u_from.id = f.user_id
        JOIN users u_to   ON u_to.id   = f.follow_id
        WHERE
            (
                :search IS NULL
                OR (
                    CASE
                        WHEN :searchType = 'ALL' THEN
                            u_from.nickname ILIKE '%' || :search || '%'
                            OR u_from.phone ILIKE '%' || :search || '%'
                            OR u_to.nickname ILIKE '%' || :search || '%'
                            OR u_to.phone ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'nickname' THEN
                            u_from.nickname ILIKE '%' || :search || '%'
            
                        WHEN :searchType = 'phone' THEN
                            u_from.phone ILIKE '%' || :search || '%'
                    END
                )
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR f.status = :status
            )
            AND (
                (:followType = 'FOLLOWING' AND u_from.uid = :uid)
                OR (:followType = 'FOLLOWERS' AND u_to.uid = :uid)
            )
        ORDER BY
            CASE WHEN :order = 'DESC' THEN f.created_at END DESC,
            CASE WHEN :order = 'ASC'  THEN f.created_at END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        followType: String,
    ): Flow<AdminFollowResponse>


}
