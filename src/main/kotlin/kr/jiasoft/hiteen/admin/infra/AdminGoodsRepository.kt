package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminGoodsRepository : CoroutineCrudRepository<GoodsGiftishowEntity, Long> {

    /**
     * 🔹 전체 개수 조회
     */
    @Query("""
        SELECT COUNT(*)
        FROM goods_giftishow g
        WHERE
        
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        g.goods_name ILIKE CONCAT('%', :search, '%')
                        OR g.brand_name ILIKE CONCAT('%', :search, '%')
                        OR g.category1_name ILIKE CONCAT('%', :search, '%')
                        OR g.goods_type_nm ILIKE CONCAT('%', :search, '%')
                        OR g.goods_type_dtl_nm ILIKE CONCAT('%', :search, '%')
                        OR g.srch_keyword ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'goodsName' AND g.goods_name ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'brandName' AND g.brand_name ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'category1Name' AND g.category1_name ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'goodsTypeName' AND (
                      g.goods_type_nm ILIKE CONCAT('%', :search, '%')
                      OR g.goods_type_dtl_nm ILIKE CONCAT('%', :search, '%')
                ))
            )
            
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND g.del_yn = 0)
                OR (:status = 'DELETED' AND g.del_yn = 1)
            )
            
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
    ): Int



    /**
     * 🔹 페이징 조회 (AdminFriendResponse)
     */
    @Query("""
        SELECT g.*
        FROM goods_giftishow g
        WHERE
        
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        g.goods_name ILIKE CONCAT('%', :search, '%')
                        OR g.brand_name ILIKE CONCAT('%', :search, '%')
                        OR g.category1_name ILIKE CONCAT('%', :search, '%')
                        OR g.goods_type_nm ILIKE CONCAT('%', :search, '%')
                        OR g.goods_type_dtl_nm ILIKE CONCAT('%', :search, '%')
                        OR g.srch_keyword ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'goodsName' AND g.goods_name ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'brandName' AND g.brand_name ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'category1Name' AND g.category1_name ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'goodsTypeName' AND (
                      g.goods_type_nm ILIKE CONCAT('%', :search, '%')
                      OR g.goods_type_dtl_nm ILIKE CONCAT('%', :search, '%')
                ))
            )
            
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND g.del_yn = 0)
                OR (:status = 'DELETED' AND g.del_yn = 1)
            )
            
        ORDER BY
            CASE WHEN :order = 'DESC' THEN g.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN g.created_at END ASC
            
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
    ): Flow<GoodsGiftishowEntity>

}