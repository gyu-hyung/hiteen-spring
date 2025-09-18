package kr.jiasoft.hiteen.feature.auth.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.auth.dto.AuthCodeRequest
import kr.jiasoft.hiteen.feature.auth.dto.VerifyRequest
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.sms.app.SmsService
import kr.jiasoft.hiteen.feature.sms.infra.SmsAuthRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val smsService: SmsService,
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val smsAuthRepository: SmsAuthRepository,
    private val jwtProvider: JwtProvider,
) {
    data class LoginForm(
        @Schema(description = "아이디") val username: String,
        @Schema(description = "비밀번호") val password: String
    )
    data class JwtResponse(
        @Schema(description = "Access Token") val token: String
    )

    //TODO 로그인 시 device 정보 user_details에 저장
    @Operation(
        summary = "로그인",
        description = "아이디/비밀번호로 로그인하여 AccessToken + RefreshToken을 발급합니다. RefreshToken은 HttpOnly 쿠키로 내려갑니다.",
        responses = [
            ApiResponse(responseCode = "200", description = "로그인 성공", content = [Content(schema = Schema(implementation = JwtResponse::class))]),
            ApiResponse(responseCode = "401", description = "로그인 실패")
        ]
    )
    @PostMapping("/login")
    suspend fun login(
        @Parameter(description = "로그인 요청 DTO") form: LoginForm,
        response: ServerHttpResponse
    ): JwtResponse =
        try {
            val (access, refresh) = authService.login(form.username, form.password)

            val cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build()

            response.addCookie(cookie)

            JwtResponse(access)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }


    @Operation(summary = "휴대폰 인증번호 발송", description = "회원가입 시 휴대폰 번호로 6자리 인증번호를 발송합니다.")
    @PostMapping("/code")
    suspend fun authCode(
        @Valid @Parameter(description = "휴대폰 인증 요청 DTO") req: AuthCodeRequest
    ): ResponseEntity<Any> {
        val phone = req.phone.filter { it.isDigit() }

        userRepository.findByPhone(phone)?.let {
            return ResponseEntity.ok(mapOf("message" to "이전에 가입한 회원이 있어~", "success" to false))
        }

        val code = (100000..999999).random().toString()
        val message = "[${System.getenv("APP_NAME") ?: "서비스"}] 회원가입 인증번호는 [$code] 입니다."

        val success = smsService.sendPhone(phone, message, code)
        if (success) {
            return ResponseEntity.ok(mapOf("message" to "인증번호를 발송했어~", "success" to true))
        }
        return ResponseEntity.ok(mapOf("message" to "인증번호 발송이 실패했어~", "success" to false))
    }


    @Operation(summary = "휴대폰 인증번호 검증", description = "휴대폰으로 받은 인증번호를 검증합니다.")
    @PostMapping("/verify")
    suspend fun authVerify(
        @Valid @Parameter(description = "휴대폰 인증 검증 요청 DTO") req: VerifyRequest
    ): ResponseEntity<Any> {
        val limit = 5
        val phone = req.phone.filter { it.isDigit() }

        //TODO: 회원가입 휴대폰 인증목적으로 요청한 코드인지 확인해야하나?
        val data = smsAuthRepository.findValidAuthCode(phone, limit) ?: return ResponseEntity.badRequest()
            .body(mapOf("code" to listOf("인증번호가 만료되었거나 유효하지 않아~")))

        if (data.code != req.code) {
            return ResponseEntity.badRequest().body(mapOf("code" to listOf("인증번호가 일치하지 않아~")))
        }

        val updated = data.copy(status = "Verified")
        smsAuthRepository.save(updated)

        return ResponseEntity.ok(mapOf("message" to "인증이 완료됐어~", "success" to true))
    }


    @Operation(
        summary = "토큰 갱신",
        description = "RefreshToken을 사용해 새로운 AccessToken을 발급합니다. 새 RefreshToken은 HttpOnly 쿠키로 내려갑니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/refresh")
    fun refresh(
        @Parameter(description = "Refresh Token") refreshToken: String
    ): ResponseEntity<ApiResult<Map<String, String>>> {
        val (access, refresh) = jwtProvider.refreshTokens(BearerToken(refreshToken))

        val cookie = ResponseCookie.from("refreshToken", refresh.value)
            .httpOnly(true)
            .path("/")
            .maxAge(Duration.ofDays(30))
            .build()

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResult.success(mapOf("accessToken" to access.value)))
    }
}
