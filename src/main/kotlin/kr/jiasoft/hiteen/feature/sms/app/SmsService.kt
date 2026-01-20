package kr.jiasoft.hiteen.feature.sms.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.feature.sms.domain.SmsAuthEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsProperties
import kr.jiasoft.hiteen.feature.sms.domain.SmsDetailEntity
import kr.jiasoft.hiteen.feature.sms.dto.AligoResponse
import kr.jiasoft.hiteen.feature.sms.infra.SmsAuthRepository
import kr.jiasoft.hiteen.feature.sms.infra.SmsDetailRepository
import kr.jiasoft.hiteen.feature.sms.infra.SmsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.time.OffsetDateTime

@Service
class SmsService(
    private val props: SmsProperties,
    private val webClient: WebClient,
    private val smsRepository: SmsRepository,
    private val smsAuthRepository: SmsAuthRepository,
    private val smsDetailRepository: SmsDetailRepository,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    enum class Status {WAITING, VERIFIED}

    data class BulkSendResult(
        val total: Int,
        val success: Int,
        val failure: Int,
        val messageId: String? = null,
        val raw: AligoResponse? = null,
    )

    /** 기본 SMS 전송 */
    suspend fun sendSms(sms: SmsEntity?, phone: String, message: String): Boolean {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("user_id", props.userId)
            add("key", props.apiKey)
            add("msg", message)
            add("receiver", phone)
            add("sender", props.callback)
            add("msg_type", if (message.length > 90) "LMS" else "SMS")
        }

        // 알리고 API 호출
        val response = webClient.post()
            .uri(props.apiUrl)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()

        val result: AligoResponse = objectMapper.readValue(response)
        println("result = ${result}")

        val success = result.isSuccess()

        // 수신자별 결과 저장(sms_details)
        if (sms != null) {
            smsDetailRepository.save(
                SmsDetailEntity(
                    smsId = sms.id,
                    phone = phone,
                    success = if (success) 1 else 0,
                    error = if (success) null else (result.message ?: "send failed"),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
                )
            )
        }

        // DB에 로그 업데이트
        if (sms != null) {
            smsRepository.save(
                sms.copy(
                    success = if (success) 1 else 0,
                    failure = if (success) 0 else 1,
                    updatedAt = sms.updatedAt
                )
            )
        }

        return success
    }

    /**
     * 알리고 bulk 문자 전송
     * - receiver 에 콤마로 최대 1000명
     * - 성공/실패 건수는 success_cnt/error_cnt 로 집계
     */
    suspend fun sendSmsBulk(sms: SmsEntity?, phones: List<String>, message: String, title: String? = null): BulkSendResult {
        val normalized = phones
            .asSequence()
            .map { it.filter(Char::isDigit) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()

        if (normalized.isEmpty()) return BulkSendResult(total = 0, success = 0, failure = 0)
        require(normalized.size <= 1000) { "phones must be <= 1000 per request" }

        val receiver = normalized.joinToString(",")

        val form = LinkedMultiValueMap<String, String>().apply {
            add("user_id", props.userId)
            add("key", props.apiKey)
            add("msg", message)
            add("receiver", receiver)
            add("sender", props.callback)
            if (!title.isNullOrBlank()) add("title", title)
            add("msg_type", if (message.length > 90) "LMS" else "SMS")
        }

        val response = webClient.post()
            .uri(props.apiUrl)
            .body(BodyInserters.fromFormData(form))
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()

        val result: AligoResponse = objectMapper.readValue(response)

        val successCnt = result.success_cnt ?: if (result.isSuccess()) normalized.size else 0
        val errorCnt = result.error_cnt ?: (normalized.size - successCnt)

        // bulk는 번호별 성공/실패를 응답에서 알 수 없으므로,
        // result_code 기준으로 요청 전체 성공/실패로 간주하여 수신자별 로그를 동일 상태로 저장
        if (sms != null) {
            val ok = result.isSuccess()
            val errMsg = if (ok) null else (result.message ?: "bulk send failed")
            val now = OffsetDateTime.now()
            normalized.forEach { phone ->
                smsDetailRepository.save(
                    SmsDetailEntity(
                        smsId = sms.id,
                        phone = phone,
                        success = if (ok) 1 else 0,
                        error = errMsg,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }
        }

        // sms 요약은 Admin 쪽에서 최종 집계로 덮어쓰므로(일괄 1건), 여기서 저장 업데이트는 하지 않음.

        return BulkSendResult(
            total = normalized.size,
            success = successCnt,
            failure = errorCnt,
            messageId = result.msg_id,
            raw = result,
        )
    }

    /** 인증번호 포함 SMS */
    suspend fun sendPhone(phone: String, message: String, authCode: String?): Boolean {
        val sms = smsRepository.save(
            SmsEntity(
                callback = phone,
                content = message,
                total = 1,
                createdAt = OffsetDateTime.now(),
            )
        )

        if (authCode != null) {
            smsAuthRepository.save(
                SmsAuthEntity(
                    smsId = sms.id,
                    phone = phone,
                    code = authCode,
                    status = Status.WAITING.name,
                    createdAt = OffsetDateTime.now()
                )
            )
        }

        return sendSms(sms, phone, message)
    }
}