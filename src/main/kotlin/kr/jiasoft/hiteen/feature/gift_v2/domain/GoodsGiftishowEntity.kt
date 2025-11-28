package kr.jiasoft.hiteen.feature.gift_v2.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("goods_giftishow")
@Schema(description = "기프티쇼 상품 정보 엔티티")
data class GoodsGiftishowEntity(

    @Id
    @field:Schema(description = "PK")
    val id: Long = 0,

    @field:Schema(description = "기프티쇼 상품 번호")
    @Column("goods_no")
    val goodsNo: Int,

    @field:Schema(description = "상품 코드 (Unique)")
    @Column("goods_code")
    val goodsCode: String,

    @field:Schema(description = "상품명")
    @Column("goods_name")
    val goodsName: String? = null,

    @field:Schema(description = "브랜드 코드")
    @Column("brand_code")
    val brandCode: String? = null,

    @field:Schema(description = "브랜드명")
    @Column("brand_name")
    val brandName: String? = null,

    @field:Schema(description = "상품 설명")
    val content: String? = null,

    @field:Schema(description = "추가 설명")
    @Column("content_add_desc")
    val contentAddDesc: String? = null,

    @field:Schema(description = "검색 키워드")
    @Column("srch_keyword")
    val searchKeyword: String? = null,

    @field:Schema(description = "MD 코드")
    @Column("md_code")
    val mdCode: String? = null,

    @field:Schema(description = "1차 카테고리 번호")
    @Column("category1_seq")
    val category1Seq: Int? = null,

    @field:Schema(description = "1차 카테고리명")
    @Column("category1_name")
    val category1Name: String? = null,

    @field:Schema(description = "상품 유형 코드")
    @Column("goods_type_cd")
    val goodsTypeCode: String? = null,

    @field:Schema(description = "상품 유형명")
    @Column("goods_type_nm")
    val goodsTypeName: String? = null,

    @field:Schema(description = "상품 상세 유형명")
    @Column("goods_type_dtl_nm")
    val goodsTypeDetailName: String? = null,

    @field:Schema(description = "이미지 (S)")
    @Column("goods_img_s")
    val goodsImgS: String? = null,

    @field:Schema(description = "이미지 (B)")
    @Column("goods_img_b")
    val goodsImgB: String? = null,

    @field:Schema(description = "웹 상세이미지")
    @Column("goods_desc_img_web")
    val goodsDescImgWeb: String? = null,

    @field:Schema(description = "브랜드 아이콘 이미지")
    @Column("brand_icon_img")
    val brandIconImg: String? = null,

    @field:Schema(description = "MMS 이미지")
    @Column("mms_goods_img")
    val mmsGoodsImg: String? = null,

    @field:Schema(description = "권장소비자가격")
    @Column("sale_price")
    val salePrice: Int = 0,

    @field:Schema(description = "실 판매가")
    @Column("real_price")
    val realPrice: Int = 0,

    @field:Schema(description = "할인율")
    @Column("discount_rate")
    val discountRate: Double = 0.0,

    @field:Schema(description = "최종 구매가격")
    @Column("discount_price")
    val discountPrice: Int = 0,

    @field:Schema(description = "공급사 ID")
    @Column("goods_com_id")
    val goodsComId: String? = null,

    @field:Schema(description = "공급사명")
    @Column("goods_com_name")
    val goodsComName: String? = null,

    @field:Schema(description = "MMS 바코드 생성 여부")
    @Column("mms_barcd_create_yn")
    val mmsBarcodeCreateYn: String? = null,

    @field:Schema(description = "교환처 ID")
    @Column("affiliate_id")
    val affiliateId: String? = null,

    @field:Schema(description = "교환처명")
    val affiliate: String? = null,

    @field:Schema(description = "전시 성별 코드")
    @Column("exh_gender_cd")
    val exhGenderCode: String? = null,

    @field:Schema(description = "전시 연령 코드")
    @Column("exh_age_cd")
    val exhAgeCode: String? = null,

    @field:Schema(description = "유효기간 유형 (01:일수/02:일자)")
    @Column("valid_prd_type_cd")
    val validPeriodType: String = "01",

    @field:Schema(description = "유효기간(일수)")
    @Column("limit_day")
    val limitDay: Int? = null,

    @field:Schema(description = "유효기간(특정일)")
    @Column("valid_prd_day")
    val validPeriodDay: String? = null,

    @field:Schema(description = "상품 상태 코드")
    @Column("goods_state_cd")
    val goodsStateCode: String? = null,

    @field:Schema(description = "판매 종료일")
    @Column("end_date")
    val endDate: OffsetDateTime? = null,

    @Column("gold_price")
    val goldPrice: Int? = null,

    @Column("vip_price")
    val vipPrice: Int? = null,

    @Column("platinum_price")
    val platinumPrice: Int? = null,

    @Column("gold_discount_rate")
    val goldDiscountRate: Double? = null,

    @Column("vip_discount_rate")
    val vipDiscountRate: Double? = null,

    @Column("platinum_discount_rate")
    val platinumDiscountRate: Double? = null,

    @Column("rm_cnt_flag")
    val rmCntFlag: String? = null,

    @Column("rm_id_buy_cnt_flag_cd")
    val rmIdBuyCntFlagCd: String? = null,

    @Column("sale_date_flag_cd")
    val saleDateFlagCd: String? = null,

    @Column("sale_date_flag")
    val saleDateFlag: String? = null,

    @Column("mms_reserve_flag")
    val mmsReserveFlag: String? = null,

    @Column("del_yn")
    val delYn: Int = 0,

    val status: Int = 0,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)