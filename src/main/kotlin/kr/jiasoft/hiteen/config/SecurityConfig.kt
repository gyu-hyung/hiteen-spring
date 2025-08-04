package kr.jiasoft.hiteen.config

import kr.jiasoft.hiteen.feature.jwt.JwtAuthenticationManager
import kr.jiasoft.hiteen.feature.jwt.JwtServerAuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
) {

    @Bean
    fun passwordEncoder (): PasswordEncoder = BCryptPasswordEncoder()


    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        authManager: JwtAuthenticationManager,
        converter: JwtServerAuthenticationConverter,): SecurityWebFilterChain {

        val filter = AuthenticationWebFilter(authManager)
        filter.setServerAuthenticationConverter(converter)

        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .addFilterAt(filter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange {
                it.pathMatchers(HttpMethod.POST,"/api/user/login", "/api/user", "/favicon.ico", "/swagger-ui/**", "/api-docs/**").permitAll()
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
}
