package kr.jiasoft.hiteen.feature.integration.tbmq.credential.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "MQTT Credential -- 미사용", description = "MQTT 임시 크리덴셜 발급/관리 API")
@RestController
@RequestMapping("/api/mqtt/credentials")
@Validated
@SecurityRequirement(name = "bearerAuth")   // 🔑 JWT 인증 필요
class MqttCredentialController(
    private val service: MqttCredentialService
) {
    data class IssueReq(
        @field:NotBlank
        @Schema(description = "디바이스 ID", example = "device-12345")
        val deviceId: String
    )

    data class IssueRes(
        @Schema(description = "MQTT 브로커 호스트", example = "mqtt.example.com")
        val host: String,
        @Schema(description = "포트 번호", example = "1883")
        val port: Int,
        @Schema(description = "TLS 사용 여부", example = "true")
        val tls: Boolean,
        @Schema(description = "클라이언트 ID", example = "client-xyz")
        val clientId: String?,
        @Schema(description = "MQTT 접속용 유저명", example = "user-abc")
        val username: String,
        @Schema(description = "MQTT 접속용 비밀번호", example = "p@ssw0rd")
        val password: String,
        @Schema(description = "퍼블리시 가능한 토픽", example = "devices/device-12345/pub")
        val pubTopic: String,
        @Schema(description = "만료 시각 (ISO-8601)", example = "2025-09-17T12:00:00Z")
        val expiresAt: String
    )

    @Operation(summary = "MQTT 크리덴셜 발급", description = "사용자 계정과 디바이스 ID로 MQTT 접속용 크리덴셜을 발급합니다.")
    @PostMapping
    suspend fun issue(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "크리덴셜 발급 요청 DTO") req: IssueReq
    ): IssueRes {
        val r = service.issueForUser(user.id, user.username, req.deviceId)
        return IssueRes(
            host = r.host, port = r.port, tls = r.tls,
            clientId = r.clientId, username = r.username, password = r.password,
            pubTopic = r.pubTopic, expiresAt = r.expiresAt?.toString() ?: ""
        )
    }

    @Operation(summary = "MQTT 크리덴셜 폐기", description = "사용자 계정과 연결된 MQTT 크리덴셜을 폐기합니다.")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun revoke(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) {
        service.revokeForUser(user.id)
    }
}
