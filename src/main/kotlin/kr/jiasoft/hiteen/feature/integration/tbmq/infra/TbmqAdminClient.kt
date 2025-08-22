package kr.jiasoft.hiteen.feature.integration.tbmq.infra

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

@Component
class TbmqAdminClient(
    private val props: TbmqProperties,
    @Qualifier("tbmqWebClient") private val client: WebClient,
    private val mapper: ObjectMapper,
) {
    private val lock = Mutex()
    @Volatile private var token: String? = null
    @Volatile private var tokenExp: Instant = Instant.EPOCH

    private data class LoginReq(val username: String, val password: String)
    private data class LoginRes(val token: String, val refreshToken: String? = null, val expiresIn: Long? = null)

    private fun isValid(now: Instant, tk: String?, exp: Instant) =
        tk != null && now.isBefore(exp.minusSeconds(60))

    private suspend fun bearer(): String {
        val now = Instant.now()
        val fastTk = token
        if (isValid(now, fastTk, tokenExp)) return fastTk!!

        return lock.withLock {
            val now2 = Instant.now()
            if (isValid(now2, token, tokenExp)) return token!!

            val res = client.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(LoginReq(props.admin.username, props.admin.password))
                .retrieve()
                .onStatus({ it.isError }) { resp ->
                    resp.bodyToMono(String::class.java).map { body ->
                        IllegalStateException("TBMQ login failed: ${resp.statusCode()} - $body")
                    }
                }
                .bodyToMono(LoginRes::class.java)
                .awaitSingle()

            token = res.token
            val ttlSec = res.expiresIn ?: (2 * 60 * 60L)
            tokenExp = now2.plusSeconds(ttlSec)
            token!!
        }
    }

    private suspend fun authBuilder() = client.mutate()
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${bearer()}")
        .build()

    /**
     * ✅ 공통 호출 유틸: 401 Unauthorized 시 토큰 무효화 후 1회 재시도
     */
    private suspend inline fun <T> callWithAuth(crossinline block: suspend (WebClient) -> T): T {
        var first = true
        while (true) {
            val wc = authBuilder()
            try {
                return block(wc)
            } catch (e: WebClientResponseException.Unauthorized) {
                if (!first) throw e
                first = false
                // 토큰 초기화 후 재시도
                lock.withLock {
                    token = null
                    tokenExp = Instant.EPOCH
                }
            }
        }
    }

    // === MQTT Basic Credentials ===
    data class AuthRules(
        val pubAuthRulePatterns: List<String> = emptyList(),
        val subAuthRulePatterns: List<String> = emptyList()
    )
    data class CredentialsValue(
        val clientId: String? = null,
        val userName: String,
        val password: String,
        val authRules: AuthRules
    )
    data class CreateCredentialsReq(
        val name: String? = null,
        val credentialsType: String = "MQTT_BASIC",
        val credentialsValue: String
    )
    data class CredentialsRes(
        val id: String,
        val name: String,
        val credentialsType: String
    )

    suspend fun createBasicCredentials(
        name: String? = null,
        clientId: String? = null,
        userName: String,
        password: String,
        pubRules: List<String>,
        subRules: List<String> = emptyList()
    ): CredentialsRes = callWithAuth { wc ->

        val value = CredentialsValue(
            clientId = clientId,
            userName = userName,
            password = password,
            authRules = AuthRules(pubRules, subRules)
        )
        val req = CreateCredentialsReq(
            name = name,
            credentialsValue = jacksonWrite(value)
        )

        wc.post().uri("/api/mqtt/client/credentials")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(req)
            .retrieve()
            .onStatus({ it.isError }) { resp ->
                resp.bodyToMono(String::class.java).map { body ->
                    IllegalStateException("Create credentials failed: ${resp.statusCode()} - $body")
                }
            }
            .bodyToMono(CredentialsRes::class.java)
            .awaitSingle()
    }

    suspend fun deleteCredentials(id: String) = callWithAuth { wc ->
        wc.delete().uri("/api/mqtt/client/credentials/{id}", id)
            .retrieve()
            .onStatus({ it.isError }) { resp ->
                resp.bodyToMono(String::class.java).map { body ->
                    IllegalStateException("Delete credentials failed: ${resp.statusCode()} - $body")
                }
            }
            .toBodilessEntity()
            .awaitSingleOrNull()
    }

    // ObjectMapper helper
    private fun jacksonWrite(any: Any): String =
        mapper.writeValueAsString(any)




    // =========== MQTT subscription update ===========





    enum class Qos { AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE }

    data class SubscriptionOptions(
        val noLocal: Boolean? = null,
        val retainAsPublish: Boolean? = null,
        /** 0,1,2 중 하나. null이면 필드 자체를 생략 */
        val retainHandling: Int? = null
    )

    data class Subscription(
        val topicFilter: String,
        val qos: Qos = Qos.AT_MOST_ONCE,
        val options: SubscriptionOptions? = null,
        /** 생략 가능; 음수는 금지. null이면 필드 자체를 생략 */
        val subscriptionId: Int? = null
    )

    data class SubscriptionReq(
        val clientId: String,
        val subscriptions: List<Subscription>
    )

    /**
     * TBMQ REST: POST /api/subscription
     * - retainHandling: 0/1/2 만 허용
     * - subscriptionId: 생략 또는 양의 정수
     */
    suspend fun addSubscriptions(clientId: String, subs: List<Subscription>) = callWithAuth { wc ->
        // 입력 유효성 최소 보정
        val sanitized = subs.map { s ->
            val rh = s.options?.retainHandling
            if (rh != null && rh !in 0..2) {
                s.copy(options = s.options.copy(retainHandling = 0)) // 기본값 0로 보정
            } else s
        }.map { s ->
            if (s.subscriptionId != null && s.subscriptionId <= 0) {
                s.copy(subscriptionId = null) // 음수/0이면 필드 제거
            } else s
        }

        val payload = SubscriptionReq(clientId = clientId, subscriptions = sanitized)

        wc.post().uri("/api/subscription")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .retrieve()
            .onStatus({ it.isError }) { resp ->
                resp.bodyToMono(String::class.java).map { body ->
                    IllegalStateException("Add subscriptions failed: ${resp.statusCode()} - $body")
                }
            }
            .toBodilessEntity()
            .awaitSingleOrNull()
    }
}
