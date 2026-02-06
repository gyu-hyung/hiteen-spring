package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminFriendResponse
import kr.jiasoft.hiteen.feature.relationship.domain.FriendEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminFriendRepository : CoroutineCrudRepository<FriendEntity, Long> {

    /**
     * 🔹 전체 개수 조회
     */
    @Query("""
        SELECT COUNT(*)
        FROM friends f
        JOIN users me ON me.uid = :uid
        JOIN users u ON (
            (f.user_id = me.id AND u.id = f.friend_id)
         OR (f.friend_id = me.id AND u.id = f.user_id)
        )
        WHERE
            (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        u.nickname ILIKE '%' || :search || '%'
                        OR u.phone ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'phone' THEN
                        u.phone ILIKE '%' || :search || '%'
                    
                    ELSE TRUE
                END
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (f.status = :status)
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
    ): Int


    /**
     * 🔹 페이징 조회 (AdminFriendResponse)
     */
    @Query("""
        SELECT 
            f.id,
            f.user_id,
            f.friend_id,
            f.status,
            f.status_at,
            f.user_location_mode,
            f.friend_location_mode,
            f.created_at,
            f.updated_at,
            f.deleted_at,
        
            -- 🔹 친구 정보 (상대방 기준)
            u.nickname AS nickname,
            u.phone AS phone,
            u.gender AS gender,
            u.birthday AS birthday,
            s.name AS school_name
        
        FROM friends f
        JOIN users me ON me.uid = :uid
        JOIN users u ON (
            (f.user_id = me.id AND u.id = f.friend_id)
         OR (f.friend_id = me.id AND u.id = f.user_id)
        )
        LEFT JOIN schools s ON u.school_id = s.id
        WHERE
            (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        u.nickname ILIKE '%' || :search || '%'
                        OR u.phone ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'nickname' THEN
                        u.nickname ILIKE '%' || :search || '%'
                    
                    WHEN :searchType = 'phone' THEN
                        u.phone ILIKE '%' || :search || '%'
                    
                    ELSE TRUE
                END
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (f.status = :status)
            )
        ORDER BY 
            CASE WHEN :order = 'DESC' THEN f.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN f.created_at END ASC
        
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
    ): Flow<AdminFriendResponse>

}
