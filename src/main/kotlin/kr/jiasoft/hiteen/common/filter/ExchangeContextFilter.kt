package kr.jiasoft.hiteen.common.filter

import kr.jiasoft.hiteen.common.context.MetaDeltaKeys
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class ExchangeContextFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        return chain.filter(exchange)
            .contextWrite { ctx ->
                ctx.put("SERVER_EXCHANGE", exchange)
                    .put(MetaDeltaKeys.DELTA_EXP, 0)
                    .put(MetaDeltaKeys.DELTA_POINT, 0)
                    .put(MetaDeltaKeys.DELTA_CASH, 0)
            }
    }
}
