package kr.jiasoft.hiteen.feature.giftishow.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsBrandEntity
import org.springframework.data.annotation.Id
import java.time.OffsetDateTime

@Schema(description = "기프티쇼 상품 조회 응답 DTO")
data class GiftishowGoodsResponse(

    @Id
    @field:Schema(description = "PK")
    val id: Long = 0,

    @field:Schema(description = "기프티쇼 상품 번호")
    val goodsNo: Int,

    @field:Schema(description = "상품 코드 (Unique)")
    val goodsCode: String,

    @field:Schema(description = "상품명")
    val goodsName: String? = null,

    @field:Schema(description = "브랜드 코드")
    val brandCode: String? = null,

    @field:Schema(description = "브랜드명")
    val brandName: String? = null,

    @field:Schema(description = "상품 설명")
    val content: String? = null,

    @field:Schema(description = "추가 설명")
    val contentAddDesc: String? = null,

    @field:Schema(description = "검색 키워드")
    val searchKeyword: String? = null,

    @field:Schema(description = "MD 코드")
    val mdCode: String? = null,

    @field:Schema(description = "1차 카테고리 번호")
    val category1Seq: Int? = null,

    @field:Schema(description = "1차 카테고리명")
    val category1Name: String? = null,

    @field:Schema(description = "상품 유형 코드")
    val goodsTypeCode: String? = null,

    @field:Schema(description = "상품 유형명")
    val goodsTypeName: String? = null,

    @field:Schema(description = "상품 상세 유형명")
    val goodsTypeDetailName: String? = null,

    @field:Schema(description = "이미지 (S)")
    val goodsImgS: String? = null,

    @field:Schema(description = "이미지 (B)")
    val goodsImgB: String? = null,

    @field:Schema(description = "웹 상세이미지")
    val goodsDescImgWeb: String? = null,

    @field:Schema(description = "브랜드 아이콘 이미지")
    val brandIconImg: String? = null,

    @field:Schema(description = "MMS 이미지")
    val mmsGoodsImg: String? = null,

    @field:Schema(description = "권장소비자가격")
    val salePrice: Int = 0,

    @field:Schema(description = "실 판매가")
    val realPrice: Int = 0,

    @field:Schema(description = "할인율")
    val discountRate: Double = 0.0,

    @field:Schema(description = "최종 구매가격")
    val discountPrice: Int = 0,

    @field:Schema(description = "공급사 ID")
    val goodsComId: String? = null,

    @field:Schema(description = "공급사명")
    val goodsComName: String? = null,

    @field:Schema(description = "MMS 바코드 생성 여부")
    val mmsBarcodeCreateYn: String? = null,

    @field:Schema(description = "교환처 ID")
    val affiliateId: String? = null,

    @field:Schema(description = "교환처명")
    val affiliate: String? = null,

    @field:Schema(description = "전시 성별 코드")
    val exhGenderCode: String? = null,

    @field:Schema(description = "전시 연령 코드")
    val exhAgeCode: String? = null,

    @field:Schema(description = "유효기간 유형 (01:일수/02:일자)")
    val validPeriodType: String = "01",

    @field:Schema(description = "유효기간(일수)")
    val limitDay: Int? = null,

    @field:Schema(description = "유효기간(특정일)")
    val validPeriodDay: String? = null,

    @field:Schema(description = "상품 상태 코드")
    val goodsStateCode: String? = null,

    @field:Schema(description = "판매 종료일")
    val endDate: OffsetDateTime? = null,

    val goldPrice: Int? = null,

    val vipPrice: Int? = null,

    val platinumPrice: Int? = null,

    val goldDiscountRate: Double? = null,

    val vipDiscountRate: Double? = null,

    val platinumDiscountRate: Double? = null,

    val rmCntFlag: String? = null,

    val rmIdBuyCntFlagCd: String? = null,

    val saleDateFlagCd: String? = null,

    val saleDateFlag: String? = null,

    val mmsReserveFlag: String? = null,

    val delYn: Int = 0,

    val status: Int = 0,

    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    val updatedAt: OffsetDateTime? = null,

    val deletedAt: OffsetDateTime? = null,

    // 브랜드
    val brand: GoodsBrandEntity?,

//     카테고리
//    val category: CategoryDto?
)
