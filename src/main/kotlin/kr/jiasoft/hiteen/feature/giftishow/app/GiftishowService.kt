package kr.jiasoft.hiteen.feature.giftishow.app

import kr.jiasoft.hiteen.feature.giftishow.domain.*
import kr.jiasoft.hiteen.feature.giftishow.dto.*
import kr.jiasoft.hiteen.feature.giftishow.infra.*
import org.springframework.stereotype.Service

@Service
class GiftishowService(
    private val goodsRepo: GiftishowGoodsRepository,
    private val brandRepo: GoodsBrandRepository,
    private val categoryRepo: GoodsCategoryRepository
) {

    /** 🔍 상품 단일 조회 */
    suspend fun getGoods(goodsCode: String): GiftishowGoodsResponse? {
        val goods = goodsRepo.findByGoodsCode(goodsCode)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다. ($goodsCode)")

//        val brand = goods.brandCode?.let { brandRepo.findByBrandCode(it) }
//        val category = goods.category1Seq?.let { categoryRepo.findBySeq(it) }

        return goods.toResponse(brand = null, category = null)
    }

    /** 📌 전체 상품 목록 조회 */
    suspend fun getGoodsList(): List<GiftishowGoodsResponse> {
        val goodsList = goodsRepo.findAllByOrderByCreatedAtDesc()

        return goodsList.map { goods ->
//            val brand = goods.brandCode?.let { brandRepo.findByBrandCode(it) }
//            val category = goods.category1Seq?.let { categoryRepo.findBySeq(it) }

            goods.toResponse(brand = null, category = null)
        }
    }

    /** ✨ Entity → Response 변환 확장 함수 */
    private fun GoodsGiftishowEntity.toResponse(
        brand: GoodsBrandEntity?,
        category: GoodsCategoryEntity?
    ) = GiftishowGoodsResponse(
        id = this.id,
        goodsNo = this.goodsNo,
        goodsCode = this.goodsCode,
        goodsName = this.goodsName,
        brandCode = this.brandCode,
        brandName = this.brandName,
        content = this.content,
        contentAddDesc = this.contentAddDesc,
        searchKeyword = this.searchKeyword,
        mdCode = this.mdCode,
        category1Seq = this.category1Seq,
        category1Name = this.category1Name,
        goodsTypeCode = this.goodsTypeCode,
        goodsTypeName = this.goodsTypeName,
        goodsTypeDetailName = this.goodsTypeDetailName,
        goodsImgS = this.goodsImgS,
        goodsImgB = this.goodsImgB,
        goodsDescImgWeb = this.goodsDescImgWeb,
        brandIconImg = this.brandIconImg,
        mmsGoodsImg = this.mmsGoodsImg,
        salePrice = this.salePrice,
        realPrice = this.realPrice,
        discountRate = this.discountRate,
        discountPrice = this.discountPrice,
        goodsComId = this.goodsComId,
        goodsComName = this.goodsComName,
        mmsBarcodeCreateYn = this.mmsBarcodeCreateYn,
        affiliateId = this.affiliateId,
        affiliate = this.affiliate,
        exhGenderCode = this.exhGenderCode,
        exhAgeCode = this.exhAgeCode,
        validPeriodType = this.validPeriodType,
        limitDay = this.limitDay,
        validPeriodDay = this.validPeriodDay,
        goodsStateCode = this.goodsStateCode,
        endDate = this.endDate,
        goldPrice = this.goldPrice,
        vipPrice = this.vipPrice,
        platinumPrice = this.platinumPrice,
        goldDiscountRate = this.goldDiscountRate,
        vipDiscountRate = this.vipDiscountRate,
        platinumDiscountRate = this.platinumDiscountRate,
        rmCntFlag = this.rmCntFlag,
        rmIdBuyCntFlagCd = this.rmIdBuyCntFlagCd,
        saleDateFlagCd = this.saleDateFlagCd,
        saleDateFlag = this.saleDateFlag,
        mmsReserveFlag = this.mmsReserveFlag,
        delYn = this.delYn,
        status = this.status,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        deletedAt = this.deletedAt,
        brand = brand,
    )
}
