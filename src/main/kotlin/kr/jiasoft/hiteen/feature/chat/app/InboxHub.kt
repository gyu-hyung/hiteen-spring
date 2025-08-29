package kr.jiasoft.hiteen.feature.chat.app

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class InboxHub {

    private val log = LoggerFactory.getLogger(javaClass)

    private data class UserChannel(
        val sink: Sinks.Many<String>,
        val refs: AtomicInteger = AtomicInteger(0),
        val lock: Any = Any() // 동시 emit 직렬화용
    )

    private val users = ConcurrentHashMap<Long, UserChannel>()

    private fun newSink(): Sinks.Many<String> =
        // 멀티캐스트 + best-effort (수요 없으면 드랍)
        Sinks.many().multicast().directBestEffort()

    fun subscribe(userId: Long): Flux<String> {
        val ch = users.compute(userId) { _, old -> old ?: UserChannel(newSink()) }!!
        ch.refs.incrementAndGet()

        return ch.sink.asFlux()
            .doOnSubscribe { log.debug("inbox subscribe userId={}", userId) }
            .doFinally {
                val remain = ch.refs.decrementAndGet()
                log.debug("inbox unsubscribe userId={} remain={}", userId, remain)
                if (remain == 0) {
                    // 마지막 구독 해지 → sink 폐기(상태 초기화)
                    users.remove(userId, ch)
                    ch.sink.tryEmitComplete()
                }
            }
    }

    fun publishTo(userId: Long, json: String) {
        val ch = users[userId] ?: return

        synchronized(ch.lock) {
            var res = ch.sink.tryEmitNext(json)
            if (res != Sinks.EmitResult.OK) {
                log.warn("inbox emit fail userId={} result={}", userId, res)

                // 구독자 없음/종료/오버플로우 등 문제 있는 sink면 교체
                if (res == Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER ||
                    res == Sinks.EmitResult.FAIL_TERMINATED ||
                    res == Sinks.EmitResult.FAIL_OVERFLOW) {

                    val replacement = users.compute(userId) { _, cur ->
                        if (cur == null || cur !== ch) cur else {
                            cur.sink.tryEmitComplete()
                            UserChannel(newSink(), cur.refs, cur.lock)
                        }
                    }

                    replacement?.let {
                        res = it.sink.tryEmitNext(json)
                        if (res != Sinks.EmitResult.OK) {
                            log.error("inbox emit retry fail userId={} result={}", userId, res)
                        }
                    }
                } else if (res == Sinks.EmitResult.FAIL_NON_SERIALIZED) {
                    // synchronized 덕에 거의 없지만, 한 번 더 시도
                    res = ch.sink.tryEmitNext(json)
                    if (res != Sinks.EmitResult.OK) {
                        log.warn("inbox emit retry after NON_SERIALIZED failed userId={} result={}", userId, res)
                    }
                }
            }
        }
    }
}
