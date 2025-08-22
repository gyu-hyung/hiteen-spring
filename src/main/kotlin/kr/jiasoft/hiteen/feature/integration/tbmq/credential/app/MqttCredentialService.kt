package kr.jiasoft.hiteen.feature.integration.tbmq.credential.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.integration.tbmq.infra.TbmqAdminClient
import kr.jiasoft.hiteen.feature.integration.tbmq.infra.TbmqProperties
import kr.jiasoft.hiteen.feature.integration.tbmq.credential.domain.MqttCredEntity
import kr.jiasoft.hiteen.feature.integration.tbmq.credential.infra.MqttCredRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class MqttCredentialService(
    private val props: TbmqProperties,
    private val client: TbmqAdminClient,
    private val encoder: PasswordEncoder,
    private val repository: MqttCredRepository
) {
    private val rnd = SecureRandom()

    data class IssueResult(
        val host: String, val port: Int, val tls: Boolean,
        val clientId: String?, val username: String, val password: String,
        val pubTopic: String, val expiresAt: Instant?,
        val tbmqCredentialsId: String
    )

    suspend fun issueForUser(userId: Long, usernameForTopic: String, deviceId: String): IssueResult {
        // 회전 정책: 기존 정보가 있으면 삭제 후 재발급(TODO: 혹은 유효하면 재사용하도록 분기)
        repository.findByUserId(userId)?.let { old ->
            //TODO tbmq 삭제요청이 실패할경우 tbmq 인증정보만 남게됨.
            runCatching { client.deleteCredentials(old.credentialsId) }
            repository.delete(old)
        }

        val mqttNickname = "u_${usernameForTopic}_${shortRand()}"
        val mqttClientId = deviceId
        val mqttUsername = usernameForTopic
        val mqttPassword = longRand()

        val pubTopic = "location/$usernameForTopic"
        // TBMQ REST로 MQTT_BASIC 자격증명 생성.
        val res = client.createBasicCredentials(
            name = mqttNickname,
            clientId = mqttClientId,
            userName = mqttUsername,
            password = mqttPassword,
            pubRules = listOf(pubTopic),
            subRules = emptyList()
        )

        val expiresAt = Instant.now().plus(3, ChronoUnit.DAYS) // TODO: 내부 정책 예시(3일)
        repository.save(
            MqttCredEntity(
                userId = userId,
                credentialsId = res.id,
                clientId = mqttClientId,
                username = mqttUsername,
                password = encoder.encode(mqttPassword),
                issuedAt = Instant.now(),
                expiresAt = expiresAt
            )
        )

        return IssueResult(
            host = props.mqtt.host,
            port = props.mqtt.port,
            tls = props.mqtt.tls,
            clientId = mqttClientId,
            username = mqttUsername,
            password = mqttPassword,
            pubTopic = pubTopic,
            expiresAt = expiresAt,
            tbmqCredentialsId = res.id
        )
    }

    suspend fun revokeForUser(userId: Long) {
        repository.findByUserId(userId)?.let { cred ->
            runCatching { client.deleteCredentials(cred.credentialsId) } // TBMQ 자격증명 삭제. :contentReference[oaicite:5]{index=5}
            repository.delete(cred)
        }
    }

    private suspend fun shortRand(): String = withContext(Dispatchers.Default) {
        val b = ByteArray(4).also { rnd.nextBytes(it) }
        Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    }
    private suspend fun longRand(): String = withContext(Dispatchers.Default) {
        val b = ByteArray(18).also { rnd.nextBytes(it) }
        Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    }
}
