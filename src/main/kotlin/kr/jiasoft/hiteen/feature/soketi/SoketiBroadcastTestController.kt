package kr.jiasoft.hiteen.feature.soketi

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/send")
class SoketiBroadcastTestController(
    private val broadcaster: SoketiBroadcaster
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** public 채널 테스트 */
    @GetMapping("/test")
    fun triggerTestBroadcast(): Mono<ResponseEntity<Map<String, String>>> {
        return try {
            val channel = "app-actions"
            val eventName = "home"
            val message = "Hello from Spring Boot with Soketi!"

            broadcaster.broadcast(channel, eventName, mapOf("message" to message))

            val result = mapOf(
                "status" to "ok",
                "channel" to channel,
                "event" to eventName
            )
            Mono.just(ResponseEntity.ok(result))
        } catch (e: Exception) {
            log.error("Broadcast error", e)
            val error = mapOf(
                "status" to "error",
                "message" to (e.message ?: "unknown error")
            )
            Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error))
        }
    }

    /** private 채널 테스트 */
    @GetMapping("/test2")
    fun triggerTest2Broadcast(): Mono<ResponseEntity<Map<String, String>>> {
        return try {
            val channel = "private-user.1"
            val eventName = "action"
            val message = "Hello from Spring Boot with Soketi!"

            broadcaster.broadcast(channel, eventName, mapOf("message" to message))

            val result = mapOf(
                "status" to "ok",
                "channel" to channel,
                "event" to eventName
            )
            Mono.just(ResponseEntity.ok(result))
        } catch (e: Exception) {
            log.error("Broadcast error", e)
            val error = mapOf(
                "status" to "error",
                "message" to (e.message ?: "unknown error")
            )
            Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error))
        }
    }
}
