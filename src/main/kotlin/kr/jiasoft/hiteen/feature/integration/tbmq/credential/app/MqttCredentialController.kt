package kr.jiasoft.hiteen.feature.integration.tbmq.credential.app

import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/mqtt/credentials")
@Validated
class MqttCredentialController(
    private val service: MqttCredentialService
) {
    data class IssueReq(@field:NotBlank val deviceId: String)
    data class IssueRes(
        val host: String, val port: Int, val tls: Boolean,
        val clientId: String?, val username: String, val password: String,
        val pubTopic: String, val expiresAt: String
    )

    @PostMapping
    suspend fun issue(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        req: IssueReq
    ): IssueRes {
        val r = service.issueForUser(user.id!!, user.username, req.deviceId)
        return IssueRes(
            host = r.host, port = r.port, tls = r.tls,
            clientId = r.clientId, username = r.username, password = r.password,
            pubTopic = r.pubTopic, expiresAt = r.expiresAt?.toString() ?: ""
        )
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun revoke(@AuthenticationPrincipal(expression = "user") user: UserEntity) {
        service.revokeForUser(user.id!!)
    }
}
