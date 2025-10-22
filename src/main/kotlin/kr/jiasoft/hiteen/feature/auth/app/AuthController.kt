package kr.jiasoft.hiteen.feature.auth.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.auth.dto.AuthCodeRequest
import kr.jiasoft.hiteen.feature.auth.dto.JwtResponse
import kr.jiasoft.hiteen.feature.auth.dto.VerifyRequest
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.sms.app.SmsService
import kr.jiasoft.hiteen.feature.sms.infra.SmsAuthRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.ResetPasswordRequest
import kr.jiasoft.hiteen.feature.user.dto.UserResponseWithTokens
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import kr.jiasoft.hiteen.validation.ValidPassword
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,

    private val userRepository: UserRepository,
    private val smsAuthRepository: SmsAuthRepository,

    private val smsService: SmsService,
    private val authService: AuthService,
) {

    @Schema(description = "로그인 요청 DTO")
    data class LoginForm(
        @Schema(description = "아이디") val phone: String,
        @Schema(description = "비밀번호") val password: String
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
    ): ResponseEntity<ApiResult<UserResponseWithTokens>> =
        try {
            val userResponseWithTokens = authService.login(form.phone, form.password)

            val cookie = ResponseCookie.from("refreshToken", userResponseWithTokens.tokens.refreshToken.toString())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build()

            response.addCookie(cookie)

            ResponseEntity.ok(ApiResult.success(userResponseWithTokens))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }


    @Operation(summary = "휴대폰 인증번호 발송", description = "회원가입 시 휴대폰 번호로 6자리 인증번호를 발송합니다.")
    @PostMapping("/code")
    suspend fun authCode(
        @Valid @Parameter(description = "휴대폰 인증 요청 DTO") req: AuthCodeRequest
    ): ResponseEntity<ApiResult<Any>> {
        val phone = req.phone.filter { it.isDigit() }

        userRepository.findByPhone(phone)?.let {
            throw IllegalArgumentException("이전에 가입한 회원이 있어~")
        }

        val code = (100000..999999).random().toString()
        val message = "[${System.getenv("APP_NAME") ?: "서비스"}] 회원가입 인증번호는 [$code] 입니다."

        val success = smsService.sendPhone(phone, message, code)
        if (success) {
            return ResponseEntity.ok(ApiResult.success(true, "인증번호를 발송했어~"))
        }
        return ResponseEntity.internalServerError().body(ApiResult.failure("인증번호 발송 실패"))
    }


    @Operation(summary = "휴대폰 인증번호 검증", description = "휴대폰으로 받은 인증번호를 검증합니다.")
    @PostMapping("/verify")
    suspend fun authVerify(
        @Valid @Parameter(description = "휴대폰 인증 검증 요청 DTO") req: VerifyRequest
    ): ResponseEntity<ApiResult<Any>> {
        val minute = 5
        val phone = req.phone.filter { it.isDigit() }

        //TODO: 회원가입 휴대폰 인증목적으로 요청한 코드인지 확인해야하나?
        val data = smsAuthRepository.findValidAuthCode(phone, minute) ?:
            throw IllegalStateException("인증번호가 만료되었거나 유효하지 않아~")

        if (data.code != req.code) {
            throw IllegalArgumentException("인증번호가 일치하지 않아~")
        }

        val updated = data.copy(status = "Verified")
        smsAuthRepository.save(updated)

        return ResponseEntity.ok(ApiResult.success(true))
    }


    @Operation(
        summary = "토큰 갱신",
        description = "RefreshToken을 사용해 새로운 AccessToken을 발급합니다. 새 RefreshToken은 HttpOnly 쿠키로 내려갑니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/refresh")
    fun refresh(
//        @Parameter(description = "Refresh Token") @RequestParam refreshToken: String,
        @CookieValue(name = "refreshToken", required = true) refreshToken: String?,
    ): ResponseEntity<ApiResult<Map<String, String>>> {
        requireNotNull(refreshToken) { "RefreshToken cookie not found" }

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





    @Operation(summary = "비밀번호 재설정 코드 발송", description = "비밀번호를 잊은 사용자가 휴대폰으로 인증코드를 받습니다.")
    @PostMapping("/password/code")
    suspend fun sendResetPasswordCode(
        @Parameter(description = "비밀번호 재설정 코드 발송 요청 DTO") @Valid req: AuthCodeRequest
    ): ResponseEntity<ApiResult<Any>> {
        val phone = req.phone.filter { it.isDigit() }

        // 가입 여부 확인
        userRepository.findByPhone(phone)
            ?: throw IllegalStateException("가입되지 않은 번호야~")

        // 인증번호 발송
        val code = (100000..999999).random().toString()
        val message = "[${System.getenv("APP_NAME") ?: "서비스"}] 비밀번호 재설정 인증번호는 [$code] 입니다."

        val success = smsService.sendPhone(phone, message, code)
        if (success) {
            return ResponseEntity.ok(ApiResult.success(mapOf("message" to "비밀번호 재설정용 인증번호를 발송했어~")))
        }
        throw IllegalStateException("인증번호 발송을 실패했어~ 나중에 다시 시도해줘~")
    }


    data class PasswordCheckRequest(
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:ValidPassword
        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        val password: String
    )


    @Operation(summary = "비밀번호 검증", description = "비밀번호 검증용 API")
    @GetMapping("/password/valid")
    suspend fun validatePassword(
        @Parameter(description = "비밀번호 검증 DTO") @Valid req: PasswordCheckRequest
    ): ResponseEntity<ApiResult<Any>> {
        return ResponseEntity.ok(ApiResult.success(req.password, "통과"))
    }


    @Operation(summary = "비밀번호 재설정", description = "휴대폰 인증번호를 검증하고, 새 비밀번호로 변경합니다.(인증번호만 보내면 검증)")
    @PostMapping("/password/reset/valid")
    suspend fun resetPasswordValid(
        @Parameter(description = "비밀번호 재설정 요청 DTO") @Valid req: ResetPasswordRequest
    ): ResponseEntity<ApiResult<Any>> {
        val phone = req.phone.filter { it.isDigit() }

        // 인증번호 검증 (5분 유효)
        val minute = 5
        val data = smsAuthRepository.findValidAuthCode(phone, minute)
            ?: return ResponseEntity.badRequest()
                .body(ApiResult.failure("인증번호가 만료되었거나 유효하지 않아~"))

        if (data.code != req.code) {
            return ResponseEntity.badRequest()
                .body(ApiResult.failure("인증번호가 일치하지 않아~"))
        }

        userRepository.findByPhone(phone)
            ?: throw java.lang.IllegalArgumentException("가입되지 않은 번호야~")

        return ResponseEntity.ok(ApiResult.success("인증번호가 확인되었습니다."))
    }


    @Operation(summary = "비밀번호 재설정", description = "휴대폰 인증번호를 검증하고, 새 비밀번호로 변경합니다.(인증번호만 보내면 검증)")
    @PostMapping("/password/reset")
    suspend fun resetPassword(
        @Parameter(description = "비밀번호 재설정 요청 DTO") @Valid req: ResetPasswordRequest
    ): ResponseEntity<ApiResult<Any>> {
        val phone = req.phone.filter { it.isDigit() }

        // 인증번호 검증 (5분 유효)
        val minute = 5
        val data = smsAuthRepository.findValidAuthCode(phone, minute)
//            ?: return ResponseEntity.badRequest()
//                .body(mapOf("code" to listOf("인증번호가 만료되었거나 유효하지 않아~")))
            ?: return ResponseEntity.badRequest()
                .body(ApiResult.failure("인증번호가 만료되었거나 유효하지 않아~"))

        if (data.code != req.code) {
//            return ResponseEntity.badRequest()
//                .body(mapOf("code" to listOf("인증번호가 일치하지 않아~")))
            return ResponseEntity.badRequest()
                .body(ApiResult.failure("인증번호가 일치하지 않아~"))
        }

        val user = userRepository.findByPhone(phone)
            ?: return ResponseEntity.badRequest()
                .body(ApiResult.failure("가입되지 않은 번호야~"))


        if(req.newPassword != null) {
            // 비밀번호 암호화 후 업데이트
            val encodedPassword = encoder.encode(req.newPassword)
            val updated = user.copy(password = encodedPassword)
            userRepository.save(updated)

            // 인증코드 재사용 방지
            smsAuthRepository.save(data.copy(status = "Used"))

            return ResponseEntity.ok(ApiResult.success("비밀번호가 재설정되었어~"))
        } else {
            throw IllegalArgumentException("newPassword is required")
        }

    }


    @Schema(description = "비밀번호 변경 요청 DTO")
    data class passWordChangeRequest(
        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:ValidPassword
        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        val oldPassword: String,

        @field:NotBlank(message = "비밀번호는 필수입니다.")
        @field:ValidPassword
        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        val newPassword: String,
    )

    @Operation(
        summary = "비밀번호 변경 검증(로그인 상태)",
        description = "입력한 비밀번호가 현재 비밀번호와 일치한지 검증합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/password/change/valid")
    suspend fun changePasswordValid(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "비밀번호 초기화 DTO") @Valid req: PasswordCheckRequest
    ): ResponseEntity<ApiResult<Any>> {

        if (!encoder.matches(req.password, user.password)) {
            throw IllegalArgumentException("비밀번호가 일치하지 않습니다.")
        }

        return ResponseEntity.ok(ApiResult.success(req.password, "통과"))
    }


    @Operation(
        summary = "비밀번호 변경 (로그인 상태)",
        description = "현재 비밀번호를 검증하고 새 비밀번호로 변경합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/password/change")
    suspend fun changePassword(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "비밀번호 초기화 DTO") @Valid req: passWordChangeRequest
    ): ResponseEntity<ApiResult<Any>> {

        if (!encoder.matches(req.oldPassword, user.password)) {
            throw IllegalArgumentException("현재 비밀번호가 일치하지 않아~")
        }

        val updated = user.copy(password = encoder.encode(req.newPassword))
        userRepository.save(updated)

        return ResponseEntity.ok(ApiResult.success("비밀번호가 변경되었어~"))
    }


}
