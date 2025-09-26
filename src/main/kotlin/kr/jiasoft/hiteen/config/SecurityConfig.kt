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

        return http
            .csrf { it.disable() }
            .cors {  }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .addFilterAt(filter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange {
                //it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers(
                    HttpMethod.POST,
                    "/api/auth/**",
                    "/broadcasting/auth",
                    "/api/user",
                    "/favicon.ico",
                ).permitAll()

                // 다운로드는 GET 허용
                it.pathMatchers(
                    HttpMethod.GET,
                    "/api/school",
                    "/api/assets/{uid}/download",
                    "/api/user/nickname/{nickname}",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                ).permitAll()

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
            allowedOriginPatterns = listOf("http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
