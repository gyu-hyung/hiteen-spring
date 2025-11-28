package kr.jiasoft.hiteen.feature.gift_v2.app

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.GiftishowApiResponse
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.biz.GiftishowBizMoneyResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.brand.GiftishowBrandDetailResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.brand.GiftishowBrandListResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.goods.GiftishowGoodsDetailResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.goods.GiftishowGoodsListResult
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherDetailDto
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherSendRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherSendResponseDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient


@Service
class GiftshowClientImpl(
    private val objectMapper: ObjectMapper,
    @param:Value("\${giftishow.url}") private val baseUrl: String,
    @param:Value("\${giftishow.auth-key}") private val authKey: String,
    @param:Value("\${giftishow.token-key}") private val token: String,
    @param:Value("\${giftishow.user-id}") private val userId: String,
    @param:Value("\${giftishow.template-id}") private val templateId: String,
    @param:Value("\${giftishow.banner-id}") private val bannerId: String,
    @param:Value("\${giftishow.callback}") private val callbackNo: String,
) : GiftshowClient {

    private val client = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Content-Type", "application/x-www-form-urlencoded")
        .build()

    private fun baseForm(apiCode: String): LinkedHashMap<String, String> =
        linkedMapOf(
            "api_code" to apiCode,
            "custom_auth_code" to authKey,
            "custom_auth_token" to token,
            "dev_yn" to "N"
        )



    override suspend fun listGoods(start: String, size: String): GiftishowApiResponse<GiftishowGoodsListResult> {
        val form = baseForm("0101").apply {
            put("start", start)
            put("size", size)
        }
        return request("/goods", form)
    }


    override suspend fun detailGoods(goodsCode: String): GiftishowApiResponse<GiftishowGoodsDetailResult> {
        val form = baseForm("0111")

        return request("/goods/$goodsCode", form)
    }


    override suspend fun listBrand(): GiftishowApiResponse<GiftishowBrandListResult>
        = request("/brands", baseForm("0102"))


    override suspend fun detailBrand(brandCode: String): GiftishowApiResponse<GiftishowBrandDetailResult> {
        val form = baseForm("0112")
        return request("/brands/$brandCode", form)
    }


    override suspend fun detailVoucher(trId: String): GiftishowApiResponse<GiftishowVoucherDetailDto> {
        val form = baseForm("0201").apply {
            put("trId", trId)
        }
        return request("/coupons", form)
    }


    suspend fun cancelVoucher(trId: String): GiftishowApiResponse<String> {
        val form = baseForm("0202").apply {
            put("trId", trId)
            put("userId", userId)
        }
        return request("/cancel", form)
    }


    suspend fun retryVoucher(trId: String, smsFlag: String): GiftishowApiResponse<String> {
        val form = baseForm("0203").apply {
            put("trId", trId)
            put("smsFlag", smsFlag)
            put("userId", userId)
        }
        return request("/resend", form)
    }


    override suspend fun issueVoucher(req: GiftishowVoucherSendRequest): GiftishowApiResponse<GiftishowVoucherSendResponseDto> {
        val form = baseForm("0204").apply {
            put("userId", userId)
            putIfNotEmpty("goods_code", req.goodsCode)
            putIfNotEmpty("order_no", req.orderNo)
            putIfNotEmpty("mms_title", req.mmsTitle)
            putIfNotEmpty("mms_msg", req.mmsMsg)
            putIfNotEmpty("callback_no", callbackNo)
            putIfNotEmpty("phone_no", req.phoneNo)
            putIfNotEmpty("tr_id", req.trId)
            putIfNotEmpty("rev_info_yn", req.revInfoYn)
            putIfNotEmpty("rev_info_date", req.revInfoDate)
            putIfNotEmpty("rev_info_time", req.revInfoTime)
            putIfNotEmpty("template_id", templateId)
            putIfNotEmpty("banner_id", bannerId)
            putIfNotEmpty("user_id", userId) // override user id (Giftishow 요청 필수값)
            putIfNotEmpty("gubun", req.gubun)

        }
        return request("/send", form)
    }


    override suspend fun bizMoney(): GiftishowApiResponse<GiftishowBizMoneyResult> {
        val form = baseForm("0301").apply {
            put("user_id", userId)
        }
        return request("/bizmoney", form)
    }


    suspend fun sendFailCancel(trId: String): GiftishowApiResponse<String> {
        val form = baseForm("0205").apply {
            put("trId", trId)
            put("user_id", userId)
        }
        return request("/send/Fail/cancel", form)
    }






    /** ------------------------------
     *  공통 호출 메소드
     * ------------------------------ */

    private fun MutableMap<String, String>.putIfNotEmpty(key: String, value: String?) {
        if (!value.isNullOrBlank()) put(key, value)
    }


    private suspend fun call(path: String, form: Map<String, String>): String {
        val body = BodyInserters.fromFormData(form.entries.first().key, form.entries.first().value)
            .apply {
                form.entries.drop(1).forEach { entry ->
                    this.with(entry.key, entry.value)
                }
            }

        return client.post()
            .uri(path)
            .body(body)
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()
    }


    private suspend inline fun <reified T> request(path: String, form: Map<String, String>): GiftishowApiResponse<T> {
        val json = call(path, form)
        return objectMapper.readValue(json, objectMapper.typeFactory.constructParametricType(GiftishowApiResponse::class.java, T::class.java))
    }


}