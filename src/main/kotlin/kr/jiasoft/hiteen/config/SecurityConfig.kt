package kr.jiasoft.hiteen.config

import kr.jiasoft.hiteen.feature.auth.infra.JwtAuthenticationManager
import kr.jiasoft.hiteen.feature.auth.infra.JwtAuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig(
) {

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        authManager: JwtAuthenticationManager,
        converter: JwtAuthenticationConverter,): SecurityWebFilterChain {

        val filter = AuthenticationWebFilter(authManager)
        filter.setServerAuthenticationConverter(converter)

        // /api/auth/** 경로는 JWT 필터를 타지 않도록 설정 (로그인, 리프레시 등)
        filter.setRequiresAuthenticationMatcher { exchange ->
            val path = exchange.request.uri.path
            if (path.startsWith("/api/auth/refresh")) {
                ServerWebExchangeMatcher.MatchResult.notMatch()
            } else {
                ServerWebExchangeMatcher.MatchResult.match()
            }
        }

        return http
            .csrf { it.disable() }
            .cors {  }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .addFilterAt(filter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange {
                // CORS preflight
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                //it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers(
                    HttpMethod.POST,
                    "/api/auth/**",
                    "/api/auth/refresh",
                    "/broadcasting/auth",
                    "/api/user",
                    "/api/inquiry",
                    // Android deferred token issue
                    "/api/invite/deferred/issue",
                ).permitAll()

                // 다운로드는 GET 허용
                it.pathMatchers(
                    HttpMethod.GET,
                    "/api/auth/**",
                    "/api/school/**",
                    "/api/assets/{uid}/download",
                    "/api/assets/{uid}/view",
                    "/api/assets/{uid}/view/**",
                    "/api/user/nickname/{nickname}",
                    "/api/admob/callback",
                    "/api-docs/**",
                    "/assets/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/favicon.ico",
                    "/actuator/**",
                    "/api/health",
                    "/api/terms/**",
                    "/api/user/profile/ss/{id}",
                    "/api/articles/**",
                    "/api/invite/ranking",
                    "/api/invite/stats/join",

                    // Android deferred token resolve (auth optional)
                    "/api/invite/deferred/resolve",
                    "/api/app/map/keys",
                    "/.well-known/assetlinks.json"
                ).permitAll()

                // 초대 공유 링크(랜딩)
                it.pathMatchers(HttpMethod.GET, "/r/**").permitAll()

                it.pathMatchers("/api/admin/**").hasRole("ADMIN")

                it.pathMatchers("/ws/**").permitAll()

                it.anyExchange().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { exchange, _ ->
                    Mono.fromRunnable {
                        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                        exchange.response.headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                    }
                }
            }
            .build()
    }


    @Bean
    fun passwordEncoder (): PasswordEncoder = BCryptPasswordEncoder()


    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf(
                "http://localhost",
                "http://localhost:3000",
                "http://localhost:8080",
                "http://cms.hiteen.co.kr",
                "https://cms.hiteen.kr",
                "https://hiteen.kr",
            )
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
