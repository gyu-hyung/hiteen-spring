package kr.jiasoft.hiteen.feature.sms.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.feature.sms.domain.SmsAuthEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsEntity
import kr.jiasoft.hiteen.feature.sms.domain.SmsProperties
import kr.jiasoft.hiteen.feature.sms.dto.AligoResponse
import kr.jiasoft.hiteen.feature.sms.infra.SmsAuthRepository
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
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this::class.java)
    enum class Status {WAITING, VERIFIED}

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
        logger.debug("SMS successfully sent $result")

        val success = result.isSuccess()

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