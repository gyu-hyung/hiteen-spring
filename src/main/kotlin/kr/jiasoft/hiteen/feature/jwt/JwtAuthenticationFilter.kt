package kr.jiasoft.hiteen.feature.jwt

import kr.jiasoft.hiteen.feature.user.UserEntity
import kr.jiasoft.hiteen.feature.user.toClaims
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationWebFilter(
    private val jwtProvider: JwtProvider
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        var accessToken: String? = null
        var refreshToken: String? = null

        // 1. Access Token 추출
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7)
        }

        // 2. Access Token 유효성 검사
        if (accessToken != null && jwtProvider.validateToken(accessToken)) {
            val account = jwtProvider.parseAccount(accessToken)
            val auth = createAuthentication(account)
            val context = SecurityContextImpl(auth)
            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))
        }

        // 3. Access Token 만료 → Refresh Token 쿠키에서 추출
        refreshToken = request.cookies["refreshToken"]?.firstOrNull()?.value

        // 4. Refresh Token 유효성 검사 및 accessToken 재발급
        if (refreshToken != null && jwtProvider.validateToken(refreshToken)) {
            val account = jwtProvider.parseAccount(refreshToken)
            val auth = createAuthentication(account)
            val context = SecurityContextImpl(auth)
            // 신규 Access Token 발급
            val claims = account.toClaims()
            val newAccessToken = jwtProvider.createAccessToken(claims)
            response.headers.add(HttpHeaders.AUTHORIZATION, "Bearer $newAccessToken")
            // 인증 컨텍스트 등록
            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))
        }

        return chain.filter(exchange)
    }

    private fun createAuthentication(account: UserEntity): UsernamePasswordAuthenticationToken {
        val authorities = listOf(SimpleGrantedAuthority("ROLE_${account.role}"))
        return UsernamePasswordAuthenticationToken(account, null, authorities)
    }
}
