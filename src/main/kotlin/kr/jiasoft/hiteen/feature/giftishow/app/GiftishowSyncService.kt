package kr.jiasoft.hiteen.feature.giftishow.app

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.coroutineScope
import kr.jiasoft.hiteen.feature.gift.app.GiftshowClient
import kr.jiasoft.hiteen.feature.gift.dto.client.brand.GiftishowBrandDto
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsBrandEntity
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.giftishow.infra.GoodsBrandRepository
import kr.jiasoft.hiteen.feature.giftishow.infra.GoodsCategoryRepository
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsCategoryEntity
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
class GiftishowSyncService(
    private val repo: GiftishowGoodsRepository,
    private val brandRepository: GoodsBrandRepository,
    private val categoryRepository: GoodsCategoryRepository,
    private val giftishowClient: GiftshowClient,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * 상품 목록 + 상세 동기화
     */
    suspend fun syncGoods(start: Int = 1, size: Int = 50) = coroutineScope {

        logger.info("📌 [Giftishow Sync] 상품 동기화 시작 (자동 페이지 반복)")

        var page = 1
        var dataCnt = 0
        var isFirstPage = true

        while (true) {



            // ------------------------------
            // 1) 실제 기프티쇼 API 호출
            // ------------------------------
            logger.info(("🔄 [상품 리스트] page=$page, size=$size 호출"))
            val listResponse = giftishowClient.listGoods(page.toString(), size.toString())

            if (listResponse.code != "0000") {
                logger.info("❌ 상품 리스트 API 오류: ${listResponse.message}")
                return@coroutineScope
            }

            val goodsList = listResponse.result?.goodsList ?: emptyList()

            // 👉 내용이 없으면 종료
            if (goodsList.isEmpty()) {
                logger.info("⛔ 더 이상 상품 없음 — 페이지 반복 종료")
                break
            }

            // ------------------------------
            // 2) 첫 페이지에서만 기존 데이터 soft delete
            // ------------------------------
            if (isFirstPage) {
                repo.markAllDeleted()
                isFirstPage = false
            }

            // ------------------------------
            // 3) 각 상품 상세 조회 후 저장
            // ------------------------------
            logger.info("📦 page=$page 상품 수: ${goodsList.size}")
            dataCnt += goodsList.size
            goodsList.forEachIndexed { i, dto ->

                val existing = repo.findByGoodsCode(dto.goodsCode)

                // 상세조회 API 호출
                val detailResponse = giftishowClient.detailGoods(dto.goodsCode)

                val detail = detailResponse.result?.goodsDetail
                if (detailResponse.code != "0000") {
                    logger.info("⚠ 상세조회 실패 → ${dto.goodsCode}, ${detailResponse.message}")
                }

                val endDate = try {
                    dto.endDate?.let {
                        OffsetDateTime.parse(
                            it,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                        )
                    }
                } catch (_: Exception) { null }

                val entity = GoodsGiftishowEntity(
                    id = existing?.id ?: 0,
                    goodsNo = dto.goodsNo,
                    goodsCode = dto.goodsCode,
                    goodsName = dto.goodsName ?: detail?.goodsName,
                    brandCode = dto.brandCode,
                    brandName = dto.brandName,
                    content = dto.content,
                    contentAddDesc = dto.contentAddDesc,
                    searchKeyword = dto.srchKeyword,
                    mdCode = dto.mdCode,
                    endDate = endDate,

                    category1Seq = detail?.categorySeq1 ?: dto.category1Seq,
                    category1Name = detail?.categoryName1,

                    goodsTypeCode = detail?.goodsTypeCd,
                    goodsTypeName = dto.goodsTypeNm?.trim(),
                    goodsTypeDetailName = dto.goodsTypeDtlNm,

                    goodsImgS = dto.goodsImgS,
                    goodsImgB = dto.goodsImgB,
                    goodsDescImgWeb = detail?.goodsDescImgWeb,

                    brandIconImg = dto.brandIconImg,
                    mmsGoodsImg = dto.mmsGoodsImg,

                    salePrice = dto.salePrice ?: 0,
                    realPrice = dto.realPrice ?: 0,
                    discountRate = dto.discountRate?.toDouble() ?: 0.0,
                    discountPrice = dto.discountPrice ?: 0,

                    goodsStateCode = dto.goodsStateCd,
                    limitDay = dto.limitDay,
                    affiliate = dto.affiliate,
                    affiliateId = dto.affiliateId,
                    goodsComId = dto.goodsComId,
                    goodsComName = dto.goodsComName,
                    exhGenderCode = dto.exhGenderCd,
                    exhAgeCode = dto.exhAgeCd,
                    validPeriodDay = dto.validPrdDay,
                    validPeriodType = dto.validPrdTypeCd,
                    mmsReserveFlag = dto.mmsReserveFlag,
                    mmsBarcodeCreateYn = dto.mmsBarcdCreateYn,
                    rmCntFlag = dto.rmCntFlag,
                    rmIdBuyCntFlagCd = dto.rmIdBuyCntFlagCd,
                    saleDateFlagCd = dto.saleDateFlagCd,
                    saleDateFlag = dto.saleDateFlag,

                    goldPrice = detail?.goldPrice,
                    vipPrice = detail?.vipPrice,
                    platinumPrice = detail?.platinumPrice,
                    goldDiscountRate = detail?.goldDiscountRate,
                    vipDiscountRate = detail?.vipDiscountRate,
                    platinumDiscountRate = detail?.platinumDiscountRate,

                    delYn = 0
                )

                repo.save(entity)
                logger.info("✔ $i 저장 완료:  ${dto.goodsName} (${dto.goodsCode})")
            }

            page++
        }

        logger.info("🎉 상품 동기화 완료 — 총 ${dataCnt}개 업데이트")
    }


    /**
     * 브랜드 + 카테고리 동기화
     */
    suspend fun syncBrandsAndCategories() = coroutineScope {
        println("📌 [Giftishow Sync] 브랜드/카테고리 동기화 시작")

        // ------------------------------
        // 1) 기프티쇼 API 호출
        // ------------------------------
        val response = giftishowClient.listBrand()

        if (response.code != "0000") {
            println("❌ 브랜드 리스트 API 오류: ${response.message}")
            return@coroutineScope
        }

        val brandList = response.result?.brandList ?: emptyList()

        // ------------------------------
        // 2) 기존 브랜드 삭제 처리
        // ------------------------------
        brandRepository.markAllDeleted()

        val seenCategorySeq = mutableSetOf<Int>()

        brandList.forEach { dto ->
            upsertBrand(dto)

            // 카테고리 생성
            if (dto.category1Seq != null && dto.category1Name != null) {
                if (seenCategorySeq.add(dto.category1Seq)) {
                    upsertCategory(dto.category1Seq, dto.category1Name)
                }
            }
        }

        println("🎉 브랜드/카테고리 동기화 완료 — 브랜드 ${brandList.size}건, 카테고리 ${seenCategorySeq.size}건")
    }


    // ------------------------------
    // 내부 Upsert 메소드
    // ------------------------------
    private suspend fun upsertBrand(dto: GiftishowBrandDto) {
        val existing = brandRepository.findByBrandCode(dto.brandCode)

        val entity = GoodsBrandEntity(
            id = existing?.id ?: 0,
            brandSeq = dto.brandSeq,
            brandCode = dto.brandCode,
            brandName = dto.brandName,
            brandBannerImg = dto.brandBannerImg,
            brandIconImg = dto.brandIConImg,
            mmsThumbImg = dto.mmsThumImg,
            content = dto.content,
            category1Seq = dto.category1Seq,
            category1Name = dto.category1Name,
            category2Seq = dto.category2Seq,
            category2Name = dto.category2Name,
            newFlag = existing?.newFlag,
            sort = dto.sort,
            delYn = 0,
            status = existing?.status ?: 1,
            createdAt = existing?.createdAt ?: OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
            deletedAt = null
        )

        brandRepository.save(entity)
        println("✔ 브랜드 저장: ${dto.brandName} (${dto.brandCode})")
    }

    private suspend fun upsertCategory(seq: Int, name: String) {
        val existing = categoryRepository.findBySeq(seq)

        val entity = GoodsCategoryEntity(
            id = existing?.id ?: 0,
            seq = seq,
            name = name,
            sort = existing?.sort ?: 9999,
            delYn = 0,
            status = existing?.status ?: 1,
            createdAt = existing?.createdAt ?: OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
            deletedAt = null
        )

        categoryRepository.save(entity)
        println("  ↳ 카테고리 저장: $name ($seq)")
    }
}
