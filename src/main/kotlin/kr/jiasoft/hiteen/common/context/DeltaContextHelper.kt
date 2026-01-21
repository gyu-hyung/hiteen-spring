package kr.jiasoft.hiteen.common.context

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

object DeltaContextHelper {

    fun addDeltaExp(amount: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.getOrEmpty<ServerWebExchange>("SERVER_EXCHANGE")
                .orElse(null)
                ?: return@deferContextual Mono.empty()

            val prev = exchange.attributes[MetaDeltaKeys.DELTA_EXP] as? Int ?: 0
            exchange.attributes[MetaDeltaKeys.DELTA_EXP] = prev + amount
            Mono.empty()
        }

    fun addDeltaPoint(amount: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.getOrEmpty<ServerWebExchange>("SERVER_EXCHANGE")
                .orElse(null)
                ?: return@deferContextual Mono.empty()

            val prev = exchange.attributes[MetaDeltaKeys.DELTA_POINT] as? Int ?: 0
            exchange.attributes[MetaDeltaKeys.DELTA_POINT] = prev + amount
            Mono.empty()
        }

    fun addDeltaCash(amount: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.getOrEmpty<ServerWebExchange>("SERVER_EXCHANGE")
                .orElse(null)
                ?: return@deferContextual Mono.empty()

            val prev = exchange.attributes[MetaDeltaKeys.DELTA_CASH] as? Int ?: 0
            exchange.attributes[MetaDeltaKeys.DELTA_CASH] = prev + amount

            Mono.empty()
        }


    fun addDeltaTier(prevTier: Int, newTier: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.getOrEmpty<ServerWebExchange>("SERVER_EXCHANGE")
                .orElse(null)
                ?: return@deferContextual Mono.empty()

            // 변화 없으면 기록 안 함
            if (prevTier == newTier) return@deferContextual Mono.empty()

            exchange.attributes[MetaDeltaKeys.DELTA_TIER] =
                mapOf(
                    "from" to prevTier,
                    "to" to newTier
                )

            Mono.empty()
        }

    fun skipMeta(): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.getOrEmpty<ServerWebExchange>("SERVER_EXCHANGE")
                .orElse(null)
                ?: return@deferContextual Mono.empty()

            exchange.attributes[MetaDeltaKeys.SKIP_META] = true
            Mono.empty()
        }

}
