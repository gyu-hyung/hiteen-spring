package kr.jiasoft.hiteen.feature.giftishow.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.GoodsBrandDto
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.OffsetDateTime

interface GiftishowGoodsRepository : CoroutineCrudRepository<GoodsGiftishowEntity, Long> {

    suspend fun findByGoodsCode(goodsCode: String): GoodsGiftishowEntity?

    fun findAllByGoodsCodeIn(goodsCodes: List<String>): Flow<GoodsGiftishowEntity>

    @Modifying
    @Query("UPDATE goods_giftishow SET del_yn = 1 WHERE goods_code LIKE 'G%'")
    suspend fun markAllDeleted()

    @Modifying
    @Query("UPDATE goods_giftishow SET del_yn = 1, deleted_at = NOW() WHERE goods_code LIKE 'G%' AND (updated_at IS NULL OR updated_at < :since)")
    suspend fun markDeletedNotUpdatedSince(since: OffsetDateTime)

    suspend fun findAllByOrderByCreatedAtDesc(): List<GoodsGiftishowEntity>


    @Query("""
        SELECT category1_seq AS category_seq,
               MAX(category1_name) AS category_name
        FROM goods_giftishow
        WHERE status = 1 
        AND del_yn = 0
        AND category1_seq IS NOT NULL
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
          AND del_yn = 0
          AND goods_type_cd IS NOT NULL
          AND goods_type_cd <> ''
          AND goods_type_nm IS NOT NULL
          AND goods_type_nm <> ''
        ORDER BY goods_type_cd
    """)
    fun findGoodsTypes(): Flow<GoodsTypeDto>


    @Query("""
        SELECT DISTINCT
            brand_code AS brand_code,
            brand_name AS brand_name
        FROM goods_giftishow
        WHERE status = 1
          AND del_yn = 0
          AND brand_code IS NOT NULL
          AND brand_code <> ''
          AND brand_name IS NOT NULL
          AND brand_name <> ''
        ORDER BY brand_name
    """)
    fun findBrands(): Flow<GoodsBrandDto>


    /**
     * 사용자 상품 목록 조회
     * */
    @Query("""
        SELECT g.*
        FROM goods_giftishow g
        WHERE g.del_yn = 0
          AND g.status = 1
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
            AND (:brandCode IS NULL OR g.brand_code = :brandCode)
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
        brandCode: String?,
    ): Flow<GoodsGiftishowEntity>




}
