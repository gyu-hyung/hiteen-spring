package kr.jiasoft.hiteen.feature.tbmq

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
}
