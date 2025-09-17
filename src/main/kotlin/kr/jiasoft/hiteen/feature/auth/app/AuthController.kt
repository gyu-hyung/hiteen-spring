package kr.jiasoft.hiteen.feature.auth.app

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

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val smsService: SmsService,
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val smsAuthRepository: SmsAuthRepository,
    private val jwtProvider: JwtProvider,
) {
    data class LoginForm(val username: String, val password: String)
    data class JwtResponse(val token: String)


    //TODO 로그인 시 device 정보 user_details에 저장
    @PostMapping("/login")
    suspend fun login(
        form: LoginForm,
        response: ServerHttpResponse
    ): JwtResponse =
        try {
            val (access, refresh) = authService.login(form.username, form.password)

            // RefreshToken을 HttpOnly 쿠키로 세팅
            val cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(true) // https 환경 권장
                .path("/")
                .maxAge(Duration.ofDays(30)) // refreshToken 유효기간
                .build()

            response.addCookie(cookie)

            JwtResponse(access)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }

    @PostMapping("/code")
    suspend fun authCode(@Valid req: AuthCodeRequest): ResponseEntity<Any> {
        val phone = req.phone.filter { it.isDigit() }

        userRepository.findByPhone(phone)?.let {
            return ResponseEntity.ok(mapOf("message" to "이전에 가입한 회원이 있어~", "success" to false))
        }

        // 인증코드 생성
        val code = (100000..999999).random().toString()
        val message = "[${System.getenv("APP_NAME") ?: "서비스"}] 회원가입 인증번호는 [$code] 입니다."

        val success = smsService.sendPhone(phone, message, code)
        if (success) {
            return ResponseEntity.ok(mapOf("message" to "인증번호를 발송했어~", "success" to true))
        }
        return ResponseEntity.ok(mapOf("message" to "인증번호 발송이 실패했어~", "success" to false))
    }


    @PostMapping("/verify")
    suspend fun authVerify(@Valid req: VerifyRequest): ResponseEntity<Any> {
        val limit = 5//기본 5분
        val phone = req.phone.filter { it.isDigit() }

        // TODO: 회원가입 휴대폰 인증목적으로 요청한 코드인지 확인해야하나?
        val data = smsAuthRepository.findValidAuthCode(phone, limit) ?: return ResponseEntity.badRequest()
            .body(mapOf("code" to listOf("인증번호가 만료되었거나 유효하지 않아~")))

        if (data.code != req.code) {
            return ResponseEntity.badRequest().body(mapOf("code" to listOf("인증번호가 일치하지 않아~")))
        }

        val updated = data.copy(status = "Verified")
        smsAuthRepository.save(updated)

        return ResponseEntity.ok(mapOf("message" to "인증이 완료됐어~", "success" to true))
    }


    @PostMapping("/refresh")
    fun refresh(refreshToken: String): ResponseEntity<ApiResult<Map<String, String>>> {
        val (access, refresh) = jwtProvider.refreshTokens(BearerToken(refreshToken))

        // 새 RefreshToken을 HttpOnly 쿠키로 내려줌
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
