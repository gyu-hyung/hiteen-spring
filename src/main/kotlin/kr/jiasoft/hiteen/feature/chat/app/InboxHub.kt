package kr.jiasoft.hiteen.feature.chat.app

import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Component
class InboxHub {
    private data class UserChannel(val sink: Sinks.Many<String>)
    private val users = ConcurrentHashMap<Long, UserChannel>()

    fun subscribe(userId: Long): Flux<String> =
        users.computeIfAbsent(userId) {
            UserChannel(Sinks.many().multicast().onBackpressureBuffer())
        }.sink.asFlux()

    fun publishTo(userId: Long, json: String) {
        users[userId]?.sink?.tryEmitNext(json)
    }
}