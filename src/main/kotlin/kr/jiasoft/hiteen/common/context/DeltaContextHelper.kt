package kr.jiasoft.hiteen.common.context

import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

object DeltaContextHelper {

    fun addDeltaExp(amount: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.get<ServerWebExchange>("SERVER_EXCHANGE")
            val prev = exchange.attributes["deltaExp"] as? Int ?: 0
            exchange.attributes["deltaExp"] = prev + amount
            Mono.empty()
        }

    fun addDeltaPoint(amount: Int): Mono<Unit> =
        Mono.deferContextual { ctx ->
            val exchange = ctx.get<ServerWebExchange>("SERVER_EXCHANGE")
            val prev = exchange.attributes["deltaPoints"] as? Int ?: 0
            exchange.attributes["deltaPoints"] = prev + amount
            Mono.empty()
        }
}
