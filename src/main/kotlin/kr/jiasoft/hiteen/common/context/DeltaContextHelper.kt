package kr.jiasoft.hiteen.common.context

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

object DeltaContextHelper {

    fun addDeltaExp(amount: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.get<ServerWebExchange>("SERVER_EXCHANGE")
            val prev = exchange.attributes[MetaDeltaKeys.DELTA_EXP] as? Int ?: 0
            exchange.attributes[MetaDeltaKeys.DELTA_EXP] = prev + amount
            Mono.empty()
        }

    fun addDeltaPoint(amount: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.get<ServerWebExchange>("SERVER_EXCHANGE")
            val prev = exchange.attributes[MetaDeltaKeys.DELTA_POINT] as? Int ?: 0
            exchange.attributes[MetaDeltaKeys.DELTA_POINT] = prev + amount
            Mono.empty()
        }
}
