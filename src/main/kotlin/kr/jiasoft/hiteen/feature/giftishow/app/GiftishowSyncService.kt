package kr.jiasoft.hiteen.feature.giftishow.app

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.coroutineScope
import kr.jiasoft.hiteen.feature.giftishow.dto.detail.GiftishowGoodsDetailResponse
import kr.jiasoft.hiteen.feature.giftishow.dto.goods.GiftishowGoodsResponse
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.goods.domain.GoodsGiftishowEntity
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
class GiftishowSyncService(
    private val repo: GiftishowGoodsRepository,
    private val objectMapper: ObjectMapper
) {

    val mockGoodsListJson = """
            {
              "code": "0000",
              "message": null,
              "result": {
                "listNum": 8,
                "goodsList": [
                  {
                    "rmIdBuyCntFlagCd": "N",
                    "discountRate": 6,
                    "mdCode": "M000100615",
                    "endDate": "2999-12-30T15:00:00.000+0000",
                    "affiliateId": "ELEVEN",
                    "discountPrice": 750,
                    "mmsGoodsImg": "https://biz.giftishow.com/Resource/goods/G00000280811/G00000280811_250.jpg",
                    "srchKeyword": "광동)비타500, 비타민, 건강음료, 세븐일레븐",
                    "limitDay": 30,
                    "content": "내용",
                    "goodsImgB": "https://biz.giftishow.com/Resource/goods/G00000280811/G00000280811.jpg",
                    "goodsTypeNm": "일반상품(물품교환형)",
                    "exhGenderCd": "WOMAN",
                    "exhAgeCd": "10",
                    "validPrdDay": "20190814",
                    "goodsComName": "세븐일레븐",
                    "goodsName": "광동)비타500 100ml 병",
                    "mmsReserveFlag": "Y",
                    "popular": 1,
                    "goodsStateCd": "SALE",
                    "brandCode": "BR00046",
                    "goodsNo": 21445,
                    "brandName": "세븐일레븐",
                    "mmsBarcdCreateYn": "Y",
                    "salePrice": 800,
                    "brandIconImg": "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg",
                    "goodsComId": "S000002705",
                    "rmCntFlag": "N",
                    "saleDateFlagCd": "PERIOD_SALE",
                    "contentAddDesc": "",
                    "goodsCode": "G00000280811",
                    "goodsTypeDtlNm": "편의점",
                    "category1Seq": 4,
                    "goodsImgS": "https://biz.giftishow.com/Resource/goods/G00000280811/G00000280811_250.jpg",
                    "affiliate": "세븐일레븐/바이더웨이",
                    "validPrdTypeCd": "01",
                    "saleDateFlag": "N",
                    "realPrice": 800
                  }
                ]
              }
            }
            """.trimIndent()


    val mockGoodsDetailJson = """
            {
              "code": "0000",
              "message": null,
              "result": {
                "goodsDetail": {
                  "rmIdBuyCntFlagCd": "N",
                  "discountRate": 6,
                  "goldPrice": 750,
                  "mdCode": "M000100615",
                  "vipDiscountRate": 9,
                  "discountPrice": 750,
                  "mmsGoodsImg": "https://biz.giftishow.com/Resource/goods/G00000280811/G00000280811_250.jpg",
                  "limitDay": 30,
                  "content": "해당 쿠폰은 일부 점포에서는 취급하지 않는 상품일 수 있습니다.",
                  "goodsDescImgWeb": "",
                  "goodsImgB": "https://biz.giftishow.com/Resource/goods/G00000280811/G00000280811.jpg",
                  "goodsTypeNm": "일반상품(물품교환형)",
                  "categoryName1": "편의점/마트",
                  "vipPrice": 730,
                  "goodsName": "광동)비타500 100ml 병",
                  "mmsReserveFlag": "Y",
                  "goodsStateCd": "SALE",
                  "brandCode": "BR00046",
                  "goldDiscountRate": 6,
                  "goodsNo": 21445,
                  "platinumPrice": 710,
                  "brandName": "세븐일레븐",
                  "salePrice": 800,
                  "brandIconImg": "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg",
                  "rmCntFlag": "N",
                  "goodsTypeCd": "GNR",
                  "platinumDiscountRate": 11,
                  "saleDateFlagCd": "PERIOD_SALE",
                  "contentAddDesc": "",
                  "categorySeq1": 4,
                  "goodsCode": "G00000280811",
                  "goodsTypeDtlNm": "편의점",
                  "goodsImgS": "https://biz.giftishow.com/Resource/goods/G00000280811/G00000280811_250.jpg",
                  "affiliate": "세븐일레븐/바이더웨이",
                  "saleDateFlag": "N",
                  "realPrice": 800
                }
              }
            }
            """.trimIndent()


    suspend fun syncGoods() = coroutineScope {

        println("📌 [Giftishow Sync] 상품 동기화 시작")

        val listResponse = objectMapper.readValue(mockGoodsListJson, GiftishowGoodsResponse::class.java)

        if (listResponse.code != "0000") {
            println("❌ 상품 리스트 응답 오류: ${listResponse.message}")
            return@coroutineScope
        }

        val goodsList = listResponse.result?.goodsList ?: emptyList()

        // Step 1: 기존 데이터를 del_yn = 1 처리
        repo.markAllDeleted()

        // Step 2: 리스트 저장 + 상세 조회 반영
        goodsList.forEach { dto ->

            val existing = repo.findByGoodsCode(dto.goodsCode)

            // 👉 상세 Mock 데이터 파싱
            val detailResponse = objectMapper.readValue(mockGoodsDetailJson, GiftishowGoodsDetailResponse::class.java)
            val detail = detailResponse.result?.goodsDetail

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
                endDate = try {
                    dto.endDate?.let {
                        OffsetDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
                    }
                } catch (e: Exception) {
                    null
                },
                category1Seq = dto.category1Seq,
                category1Name = detail?.categoryName1,
                goodsTypeCode = detail?.goodsTypeCd,
                goodsTypeName = dto.goodsTypeNm,
                goodsTypeDetailName = dto.goodsTypeDtlNm,
                goodsImgS = dto.goodsImgS,
                goodsImgB = dto.goodsImgB,
                goodsDescImgWeb = detail?.goodsDescImgWeb,
                brandIconImg = dto.brandIconImg,
                mmsGoodsImg = dto.mmsGoodsImg,
                salePrice = dto.salePrice ?: 0,
                realPrice = dto.realPrice ?: 0,
                discountRate = dto.discountRate ?: 0.0,
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

            println("✔ 저장 완료: ${dto.goodsName} (${dto.goodsCode})")
        }

        println("🎉 상품 동기화 완료 — 총 ${goodsList.size}개 업데이트")
    }

}
