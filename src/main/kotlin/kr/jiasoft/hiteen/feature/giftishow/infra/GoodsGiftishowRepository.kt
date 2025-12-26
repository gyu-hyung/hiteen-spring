package kr.jiasoft.hiteen.feature.giftishow.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface GiftishowGoodsRepository : CoroutineCrudRepository<GoodsGiftishowEntity, Long> {

    suspend fun findByGoodsCode(goodsCode: String): GoodsGiftishowEntity?

    fun findAllByGoodsCodeIn(goodsCodes: List<String>): Flow<GoodsGiftishowEntity>

//    @Query("UPDATE goods_giftishow SET del_yn = 1 WHERE gg.goods_code LIKE 'G%'")
    @Query("UPDATE goods_giftishow gg SET deleted_at = now() WHERE gg.goods_code LIKE 'G%'")
    suspend fun markAllDeleted()

    suspend fun findAllByOrderByCreatedAtDesc(): List<GoodsGiftishowEntity>





    @Query("""
        SELECT category1_seq AS category_seq,
               MAX(category1_name) AS category_name
        FROM goods_giftishow
        WHERE status = 1 
        AND deleted_at IS NULL
        GROUP BY category1_seq
        ORDER BY category1_seq
    """)
    fun findCategories(): Flow<GoodsCategoryDto>


    @Query("""
        SELECT DISTINCT
            goods_type_cd AS goods_type_cd,
            goods_type_nm AS goods_type_name
        FROM goods_giftishow
        WHERE status = 1 
        AND deleted_at IS NULL
        ORDER BY goods_type_cd
    """)
    fun findGoodsTypes(): Flow<GoodsTypeDto>



    /**
     * 사용자 상품 목록 조회
     * */
    @Query("""
        SELECT g.*
        FROM goods_giftishow g
        WHERE deleted_at IS NULL AND status = 1
            AND (
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
            AND (:categorySeq IS NULL OR g.category1_seq = :categorySeq)
            AND (:goodsTypeCd IS NULL OR g.goods_type_cd = :goodsTypeCd)
            AND (:lastId IS NULL OR g.id < :lastId)
        ORDER BY g.id DESC
        LIMIT :size
    """)
    fun listByCursorId(
        size: Int,
        lastId: Long?,
        search: String?,
        searchType: String,
        categorySeq: Int?,
        goodsTypeCd: String?,
    ): Flow<GoodsGiftishowEntity>




}
