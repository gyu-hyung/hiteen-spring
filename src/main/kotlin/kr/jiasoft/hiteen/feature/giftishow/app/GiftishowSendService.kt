package kr.jiasoft.hiteen.feature.giftishow.app

import com.fasterxml.jackson.databind.ObjectMapper
import kr.jiasoft.hiteen.feature.giftishow.domain.GiftishowLogsEntity
import kr.jiasoft.hiteen.feature.giftishow.dto.send.GiftishowSendRequest
import kr.jiasoft.hiteen.feature.giftishow.dto.send.GiftishowSendResponse
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowLogsRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
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
                "tr_id": "202511210001"
              }
            }
            """.trimIndent()




    private val client = WebClient.create()

    suspend fun sendCoupon(
        giftUserId: Long,
        goodsCode: String,
        goodsName: String,
        orderNo: String? = null,
        mmsTitle: String,
        mmsMsg: String,
        phone: String,
        revInfoYn: String = "N",
        revInfoDate: String? = null,
        revInfoTime: String? = null,
        gubun: String = "I",
    ): GiftishowLogsEntity {

        val trId = UUID.randomUUID().toString().replace("-", "").take(16)

        val requestBody = GiftishowSendRequest(
            custom_auth_code = authKey,
            custom_auth_token = token,
            goods_code = goodsCode,
            order_no = orderNo,
            mms_title = mmsTitle,
            mms_msg = mmsMsg,
            callback_no = callbackNo,
            phone_no = phone,
            tr_id = trId,
            rev_info_yn = revInfoYn,
            rev_info_time = revInfoTime,
            template_id = templateId,
            banner_id = bannerId,
            user_id = userId,
            gubun = gubun,
        )

        // [현재는 Mock 요청] → 실제 호출 시 내부 수정
        val response = objectMapper.readValue(mockSendCouponResponse, GiftishowSendResponse::class.java)

        // ---- DB 로그 기록 ----
        val log = GiftishowLogsEntity(
            giftUserId = giftUserId,
            goodsCode = goodsCode,
            goodsName = goodsName,
            orderNo = orderNo,
            mmsMsg = requestBody.mms_msg,
            mmsTitle = requestBody.mms_title,
            callbackNo = callbackNo,
            phoneNo = phone,
            trId = trId,
            reserveYn = revInfoYn,
            reserveDate = revInfoDate,
            reserveTime = revInfoTime,
            templateId = templateId,
            bannerId = bannerId,
            userId = userId,
            gubun = gubun,
            response = mockSendCouponResponse, // 실 API에서는 실제 raw 응답
            code = response.code,
            message = response.message,
            pinNo = response.result?.pinNo,
            couponImgUrl = response.result?.couponImgUrl,
            memo = null,
            status = if (response.code == "0000") 1 else -1,
        )

        return logRepository.save(log)
    }
}
