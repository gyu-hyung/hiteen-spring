package kr.jiasoft.hiteen.common.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.location.infra.cache.LocationCacheRedisService
import kr.jiasoft.hiteen.feature.soketi.app.SoketiBroadcaster
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@Tag(name = "Health", description = "서비스 헬스체크 API ")
@RestController
@RequestMapping("/api/health")
class HealthController(
    private val locationCacheRedisService: LocationCacheRedisService,
    private val soketiBroadcaster: SoketiBroadcaster,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate, // PostgreSQL R2DBC
    private val mongoTemplate: ReactiveMongoTemplate,
) {

    @Operation(summary = "전체 시스템 헬스체크", description = "Redis, Soketi, Mongo, API 상태를 점검합니다.")
    @GetMapping
    suspend fun check(): ResponseEntity<String> {
        val sb = StringBuilder()
        sb.appendLine("🩺 HITEEN HEALTH CHECK")
        sb.appendLine("===========================")
        sb.appendLine("API     : ✅ UP")

        val redis = runCatching { locationCacheRedisService.testConnection() }.getOrNull()
        sb.appendLine("Redis   : ${if (redis == "PONG") "✅ UP ($redis)" else "❌ DOWN"}")

        val soketi = runCatching { soketiBroadcaster.testConnection() }.getOrNull()
        sb.appendLine("Soketi  : ${if (soketi == true) "✅ UP" else "❌ DOWN"}")

        // PostgreSQL (R2DBC Ping)
        val postgresOk = runCatching {
            r2dbcEntityTemplate.databaseClient.sql("SELECT 1")
                .fetch()
                .first()
                .awaitFirstOrNull()
        }.isSuccess
        sb.appendLine("PostgreSQL: ${if (postgresOk) "✅ UPS" else "❌ DOWN"}")

        val mongoOk = runCatching {
            mongoTemplate.executeCommand("""{ ping: 1 }""").awaitFirstOrNull()
        }.isSuccess
        sb.appendLine("MongoDB : ${if (mongoOk) "✅ UP" else "❌ DOWN"}")

        sb.appendLine("===========================")
        sb.appendLine("Timestamp: ${java.time.Instant.now()}")
        return ResponseEntity.ok(sb.toString())
    }

}
