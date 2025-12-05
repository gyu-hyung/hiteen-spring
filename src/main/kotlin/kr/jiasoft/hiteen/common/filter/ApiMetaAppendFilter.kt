package kr.jiasoft.hiteen.common.filter

import kr.jiasoft.hiteen.common.app.MetaAppendingResponseDecorator
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.point.infra.PointSummaryRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class ApiMetaAppendFilter(
    private val userRepository: UserRepository,
    private val tierRepository: TierRepository,
    private val pointSummaryRepository: PointSummaryRepository,
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        val principalMono = exchange.getPrincipal<Authentication>()

        return chain.filter(
            exchange.mutate().response(
                MetaAppendingResponseDecorator(
                    exchange.response,
                    exchange,                 // ★ Decorator에 전달
                    principalMono,
                    userRepository,
                    tierRepository,
                    pointSummaryRepository
                )
            ).build()
        )
    }
}
