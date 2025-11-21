package kr.jiasoft.hiteen.feature.giftishow.app

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.feature.giftishow.domain.GiftishowLogsEntity
import kr.jiasoft.hiteen.feature.giftishow.dto.send.GiftishowSendRequest
import kr.jiasoft.hiteen.feature.giftishow.dto.send.GiftishowSendResponse
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowLogsRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.time.OffsetDateTime
import java.util.UUID

@Service
class GiftishowSendService(
    private val logRepository: GiftishowLogsRepository,
    private val objectMapper: ObjectMapper,
) {

    @Value("\${giftishow.url}")
    private lateinit var baseUrl: String

    @Value("\${giftishow.auth-key}")
    private lateinit var authKey: String

    @Value("\${giftishow.token-key}")
    private lateinit var token: String

    @Value("\${giftishow.template-id}")
    private lateinit var templateId: String

    @Value("\${giftishow.banner-id}")
    private lateinit var bannerId: String

    @Value("\${giftishow.callback}")
    private lateinit var callbackNo: String

    @Value("\${giftishow.user-id}")
    private lateinit var userId: String

    private val mockSendCouponResponse = """
            {
              "code": "0000",
              "message": null,
              "result": {
                "pinNo": "GIFT1234567890",
                "couponImgUrl": "https://giftishow.com/coupon/GIFT1234567890.png",
                "tr_id": "202511210001",
                "goodsName": "스타벅스 아메리카노"
              }
            }
            """.trimIndent()




    private val client = WebClient.create()

    suspend fun sendCoupon(giftUserId: Long, goodsCode: String, phone: String, goodsName: String): GiftishowLogsEntity {

        val trId = UUID.randomUUID().toString().replace("-", "").take(16)

        val requestBody = GiftishowSendRequest(
            custom_auth_code = authKey,
            custom_auth_token = token,
            goods_code = goodsCode,
            tr_id = trId,
            phone_no = phone,
            callback_no = callbackNo,
            user_id = userId,
            mms_title = "[하이틴] 기프티콘 도착!",
            mms_msg = "$goodsName 쿠폰이 도착했어요🎁",
            banner_id = bannerId,
            template_id = templateId
        )

        // [현재는 Mock 요청] → 실제 호출 시 내부 수정
        val response = objectMapper.readValue(mockSendCouponResponse, GiftishowSendResponse::class.java)

        // ---- DB 로그 기록 ----
        val log = GiftishowLogsEntity(
            giftUserId = giftUserId,
            goodsCode = goodsCode,
            goodsName = goodsName,
            phoneNo = phone,
            trId = trId,
            callbackNo = callbackNo,
            mmsTitle = requestBody.mms_title,
            mmsMsg = requestBody.mms_msg,
            templateId = templateId,
            bannerId = bannerId,
            userId = userId,
            response = mockSendCouponResponse, // 실 API에서는 실제 raw 응답
            code = response.code,
            message = response.message,
            pinNo = response.result?.pinNo,
            couponImgUrl = response.result?.couponImgUrl,
            status = if (response.code == "0000") 1 else -1,
            createdAt = OffsetDateTime.now()
        )

        return logRepository.save(log)
    }
}
