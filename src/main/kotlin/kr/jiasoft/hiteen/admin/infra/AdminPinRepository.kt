package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminPinResponse
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.util.UUID

interface AdminPinRepository : CoroutineCrudRepository<PinEntity, Long> {



    @Query("""
        SELECT
            p.id,
            p.user_id,
            u.uid        AS user_uid,
            u.nickname   AS nickname,
            p.zipcode,
            p.lat,
            p.lng,
            p.description,
            p.visibility,
            p.created_at,
            
            CASE
                WHEN p.created_at >= NOW() - INTERVAL '24 HOURS' THEN 'ACTIVE'
                ELSE 'INACTIVE'
            END AS status
            
        FROM pin p
        JOIN users u ON u.id = p.user_id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        p.description ILIKE CONCAT('%', :search, '%')
                        OR u.nickname ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'description' AND p.description ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND p.created_at >= NOW() - INTERVAL '24 HOURS')
                OR (:status = 'INACTIVE' AND p.created_at < NOW() - INTERVAL '24 HOURS')
            )
            
            AND (
                :visibility IS NULL OR :visibility = 'ALL'
                OR (:visibility = 'PUBLIC' AND p.visibility = 'PUBLIC')
                OR (:visibility = 'PRIVATE' AND p.visibility = 'PRIVATE')
                OR (:visibility = 'FRIENDS' AND p.visibility = 'FRIENDS')   
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = p.created_id ) = :uid
            )
    
    
        ORDER BY
            CASE WHEN :order = 'DESC' THEN p.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN p.created_at END ASC
    
        LIMIT :size OFFSET (:page - 1) * :size
    """
        )
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        visibility: String?,
    ): Flow<AdminPinResponse>





    @Query("""
        SELECT COUNT(*)
        FROM pin p
        JOIN users u ON u.id = p.user_id
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        p.description ILIKE CONCAT('%', :search, '%')
                        OR u.nickname ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'description' AND p.description ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND p.created_at >= NOW() - INTERVAL '24 HOURS')
                OR (:status = 'INACTIVE' AND p.created_at < NOW() - INTERVAL '24 HOURS')
            )
            
            AND (
                :visibility IS NULL OR :visibility = 'ALL'
                OR (:visibility = 'PUBLIC' AND p.visibility = 'PUBLIC')
                OR (:visibility = 'PRIVATE' AND p.visibility = 'PRIVATE')
                OR (:visibility = 'FRIENDS' AND p.visibility = 'FRIENDS')   
            )
    
            AND (
                :uid IS NULL
                OR ( SELECT u.uid FROM users u WHERE u.id = p.created_id ) = :uid
            )
    
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        visibility: String?,
    ): Int


}

