package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.GoodsCategoryDto
import kr.jiasoft.hiteen.admin.dto.GoodsTypeDto
import kr.jiasoft.hiteen.admin.dto.GoodsBrandDto
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminGoodsRepository : CoroutineCrudRepository<GoodsGiftishowEntity, Long> {


    @Query("""
        SELECT category1_seq AS category_seq,
               MAX(category1_name) AS category_name
        FROM goods_giftishow
        WHERE category1_seq IS NOT NULL
        GROUP BY category1_seq
        ORDER BY category1_seq
    """)
    fun findCategories(): Flow<GoodsCategoryDto>


    @Query("""
        SELECT DISTINCT
            goods_type_cd AS goods_type_cd,
            goods_type_nm AS goods_type_name
        FROM goods_giftishow
        WHERE goods_type_cd IS NOT NULL
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
        WHERE brand_code IS NOT NULL
          AND brand_code <> ''
          AND brand_name IS NOT NULL
          AND brand_name <> ''
        ORDER BY brand_name
    """)
    fun findBrands(): Flow<GoodsBrandDto>


    @Query("""
        SELECT goods_code
        FROM goods_giftishow
        WHERE goods_code LIKE 'H%'
        ORDER BY goods_code DESC
        LIMIT 1
    """)
    suspend fun findMaxHGoodsCode(): String?

    @Query("""
        SELECT goods_code
        FROM goods_giftishow
        WHERE goods_code LIKE 'D%'
        ORDER BY goods_code DESC
        LIMIT 1
    """)
    suspend fun findMaxDGoodsCode(): String?


    /**
     * 🔹 전체 개수 조회
     */
    @Query("""
        SELECT COUNT(*)
        FROM goods_giftishow g
        WHERE
            (g.del_yn IS NULL OR g.del_yn = 0)
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        g.goods_name ILIKE '%' || :search || '%'
                        OR g.brand_name ILIKE '%' || :search || '%'
                        OR g.category1_name ILIKE '%' || :search || '%'
                        OR g.goods_type_nm ILIKE '%' || :search || '%'
                        OR g.goods_type_dtl_nm ILIKE '%' || :search || '%'
                        OR g.srch_keyword ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'goodsName' THEN
                        g.goods_name ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'brandName' THEN
                        g.brand_name ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'category1Name' THEN
                        g.category1_name ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'goodsTypeName' THEN
                        g.goods_type_nm ILIKE '%' || :search || '%'
                        OR g.goods_type_dtl_nm ILIKE '%' || :search || '%'
            
                    ELSE TRUE
                END
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND g.status = 1)
                OR (:status = 'DELETED' AND g.status = 0)
            )
            
            AND (:categorySeq IS NULL OR g.category1_seq = :categorySeq)
            AND (:goodsTypeCd IS NULL OR g.goods_type_cd = :goodsTypeCd)
            
            AND (
                :goodsCodeType IS NULL OR :goodsCodeType = 'ALL'
                OR g.goods_code LIKE CONCAT(:goodsCodeType, '%')
            )
            
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        categorySeq: Int?,
        goodsTypeCd: String?,
        goodsCodeType: String?,
    ): Int



    /**
     * 🔹 페이징 조회 (AdminFriendResponse)
     */
    @Query("""
        SELECT g.*
        FROM goods_giftishow g
        WHERE
            (g.del_yn IS NULL OR g.del_yn = 0)
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        g.goods_name ILIKE '%' || :search || '%'
                        OR g.brand_name ILIKE '%' || :search || '%'
                        OR g.category1_name ILIKE '%' || :search || '%'
                        OR g.goods_type_nm ILIKE '%' || :search || '%'
                        OR g.goods_type_dtl_nm ILIKE '%' || :search || '%'
                        OR g.srch_keyword ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'goodsName' THEN
                        g.goods_name ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'brandName' THEN
                        g.brand_name ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'category1Name' THEN
                        g.category1_name ILIKE '%' || :search || '%'
            
                    WHEN :searchType = 'goodsTypeName' THEN
                        g.goods_type_nm ILIKE '%' || :search || '%'
                        OR g.goods_type_dtl_nm ILIKE '%' || :search || '%'
            
                    ELSE TRUE
                END
            )
            
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND g.status = 1)
                OR (:status = 'DELETED' AND g.status = 0)
            )
            
            AND (:categorySeq IS NULL OR g.category1_seq = :categorySeq)
            AND (:goodsTypeCd IS NULL OR g.goods_type_cd = :goodsTypeCd)
            
            AND (
                :goodsCodeType IS NULL OR :goodsCodeType = 'ALL'
                OR g.goods_code LIKE CONCAT(:goodsCodeType, '%')
            )

        ORDER BY g.status DESC, g.brand_name ASC, g.sale_price ASC, g.id ASC
            
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        search: String?,
        searchType: String,
        status: String?,
        categorySeq: Int?,
        goodsTypeCd: String?,
        goodsCodeType: String?,
        ): Flow<GoodsGiftishowEntity>

    @Query("""
        UPDATE goods_giftishow
        SET status = :status,
            updated_at = NOW()
        WHERE id IN (:ids)
    """)
    suspend fun updateStatusByIds(ids: List<Long>, status: Int): Int?

}