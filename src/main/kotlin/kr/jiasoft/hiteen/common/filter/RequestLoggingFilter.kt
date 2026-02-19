package kr.jiasoft.hiteen.common.filter

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

// @Component  // Nginx에서 로깅하므로 비활성화
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : WebFilter {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val method = request.method
        val path = request.uri.path
        val query = request.uri.query?.let { "?$it" } ?: ""

        log.info("{} {}{}", method, path, query)

        return chain.filter(exchange)
    }
}

