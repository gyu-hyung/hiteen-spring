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
import kr.jiasoft.hiteen.common.exception.AlreadyRegisteredException
import kr.jiasoft.hiteen.feature.auth.dto.AuthCodeRequest
import kr.jiasoft.hiteen.feature.auth.dto.AuthPasswordCodeRequest
import kr.jiasoft.hiteen.feature.auth.dto.ChangePhoneRequest
import kr.jiasoft.hiteen.feature.auth.dto.JwtResponse
import kr.jiasoft.hiteen.feature.auth.dto.LoginForm
import kr.jiasoft.hiteen.feature.auth.dto.PassWordChangeRequest
import kr.jiasoft.hiteen.feature.auth.dto.PasswordCheckRequest
import kr.jiasoft.hiteen.feature.auth.dto.ResetPasswordValidRequest
import kr.jiasoft.hiteen.feature.auth.dto.VerifyRequest
import kr.jiasoft.hiteen.feature.auth.infra.BearerToken
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.auth.infra.JwtSessionService
import kr.jiasoft.hiteen.feature.sms.app.SmsService
import kr.jiasoft.hiteen.feature.sms.infra.SmsAuthRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.ResetPasswordRequest
import kr.jiasoft.hiteen.feature.user.dto.UserResponseWithTokens
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import kr.jiasoft.hiteen.feature.user.app.UserDetailService
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val jwtSessionService: JwtSessionService,

    private val userRepository: UserRepository,
    private val smsAuthRepository: SmsAuthRepository,

    private val smsService: SmsService,
    private val authService: AuthService,
    private val userDetailService: UserDetailService,
) {




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
    ): ResponseEntity<ApiResult<UserResponseWithTokens>> {
        val userResponseWithTokens = authService.login(form.phone, form.password)

//            val cookie = ResponseCookie.from("refreshToken", userResponseWithTokens.tokens.refreshToken.toString())
//                .httpOnly(true)
//                .secure(true)
//                .path("/")
//                .maxAge(Duration.ofDays(30))
//                .build()
//
//            response.addCookie(cookie)

        return ResponseEntity.ok(ApiResult.success(userResponseWithTokens))
    }


    @Operation(summary = "휴대폰 인증번호 발송", description = "회원가입 시 휴대폰 번호로 6자리 인증번호를 발송합니다.")
    @PostMapping("/code")
    suspend fun authCode(
        @Validated @Parameter(description = "휴대폰 인증 요청 DTO") req: AuthCodeRequest
    ): ResponseEntity<ApiResult<Any>> {
        val phone = req.phone.filter { it.isDigit() }

        userRepository.findActiveByUsername(phone)?.let {
            throw AlreadyRegisteredException("이미 가입된 번호야~")
        }

        val code = (100000..999999).random().toString()
        val message = "[하이틴] 회원가입 인증번호는 [$code] 입니다."

        val success = smsService.sendPhone(phone, message, code)
        if (success) {
            return ResponseEntity.ok(ApiResult.success(true, "인증번호를 발송했어~"))
        } else {
            throw IllegalStateException("인증번호 발송 실패")
        }
    }


    @Operation(summary = "휴대폰 인증번호 검증", description = "휴대폰으로 받은 인증번호를 검증합니다.")
    @PostMapping("/verify")
    suspend fun authVerify(
        @Validated @Parameter(description = "휴대폰 인증 검증 요청 DTO") req: VerifyRequest
    ): ResponseEntity<ApiResult<Any>> {
        val minute = 5
        val phone = req.phone.filter { it.isDigit() }

        //TODO: 회원가입 휴대폰 인증목적으로 요청한 코드인지 확인해야하나?
        val data = smsAuthRepository.findValidAuthCode(phone, minute) ?:
            throw IllegalStateException("인증번호가 만료되었거나 유효하지 않아~")

        if (data.code != req.code) {
            throw IllegalArgumentException("인증번호가 일치하지 않아~")
        }

        val updated = data.copy(status = "VERIFIED")
        smsAuthRepository.save(updated)

        return ResponseEntity.ok(ApiResult.success(true))
    }


    @Operation(
        summary = "토큰 갱신",
        description = "RefreshToken을 사용해 새로운 AccessToken을 발급합니다. 새 RefreshToken은 HttpOnly 쿠키로 내려갑니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/refresh")
    suspend fun refresh(
        @Parameter(description = "Refresh Token") @RequestParam refreshToken: String,
//        @CookieValue(name = "refreshToken", required = true) refreshToken: String?,
    ): ResponseEntity<ApiResult<Map<String, String>>> {
        requireNotNull(refreshToken) { "RefreshToken cookie not found." }

        val oldToken = BearerToken(refreshToken)
        val oldJti = jwtProvider.extractJti(oldToken)
        val username = jwtProvider.extractUsername(oldToken)
            ?: throw IllegalArgumentException("Invalid refresh token")

        // 🔒 기존 토큰의 jti 검증 (탈취된 토큰 사용 방지)
        if (oldJti != null && jwtSessionService.hasSession(username)) {
            if (!jwtSessionService.isValidSession(username, oldJti)) {
                throw IllegalArgumentException("Session expired. Please login again.")
            }
        }

        val (access, refresh, jti) = jwtProvider.refreshTokens(oldToken)

        // 🔒 새 세션 등록
        jwtSessionService.registerSession(username, jti)

//        val cookie = ResponseCookie.from("refreshToken", refresh.value)
//            .httpOnly(true)
//            .path("/")
//            .maxAge(Duration.ofDays(30))
//            .build()

        return ResponseEntity.ok()
//            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(ApiResult.success(mapOf("accessToken" to access.value, "refreshToken" to refresh.value)))
    }


    @Operation(
        summary = "로그아웃",
        description = "로그아웃 처리 - FCM 토큰(device_token)을 삭제하고 세션을 무효화합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/logout")
    suspend fun logout(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<Boolean>> {
        userDetailService.clearDeviceToken(user.uid)
        // 🔒 세션 무효화
        jwtSessionService.invalidateSession(user.username)
        return ResponseEntity.ok(ApiResult.success(true, "로그아웃 완료"))
    }


    @Operation(summary = "비밀번호 재설정 코드 발송", description = "비밀번호를 잊은 사용자가 휴대폰으로 인증코드를 받습니다.")
    @PostMapping("/password/code")
    suspend fun sendResetPasswordCode(
        @Parameter(description = "비밀번호 재설정 코드 발송 요청 DTO") @Validated req: AuthPasswordCodeRequest
    ): ResponseEntity<ApiResult<Any>> {
        val phone = req.phone!!.filter { it.isDigit() }

        // 가입 여부 확인
        val user = userRepository.findActiveByUsername(phone)
            ?: throw IllegalStateException("가입되지 않은 사용자야~".trimIndent())

        if(user.nickname != req.nickname) {
            throw IllegalArgumentException("""
                가입되지 않은 사용자야~
                힌트: ${user.nickname.get(0)}****
            """.trimIndent())
        }

        // 인증번호 발송
        val code = (100000..999999).random().toString()
        val message = "[하이틴] 비밀번호 재설정 인증번호는 [$code] 입니다."

        val success = smsService.sendPhone(phone, message, code)
        if (success) {
            return ResponseEntity.ok(ApiResult.success(mapOf("message" to "비밀번호 재설정용 인증번호를 발송했어~")))
        }
        throw IllegalStateException("인증번호 발송을 실패했어~ 나중에 다시 시도해줘~")
    }


    @Operation(summary = "비밀번호 검증", description = "비밀번호 검증용 API")
    @GetMapping("/password/valid")
    suspend fun validatePassword(
        @Parameter(description = "비밀번호 검증 DTO") @Validated req: PasswordCheckRequest
    ): ResponseEntity<ApiResult<Any>> {
        return ResponseEntity.ok(ApiResult.success(req.password, "통과"))
    }


    @Operation(summary = "비밀번호 재설정", description = "휴대폰 인증번호를 검증하고, 새 비밀번호로 변경합니다.(인증번호만 보내면 검증)")
    @PostMapping("/password/reset/valid")
    suspend fun resetPasswordValid(
        @Parameter(description = "비밀번호 재설정 요청 DTO") @Validated req: ResetPasswordValidRequest
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

        userRepository.findByUsername(phone)
            ?: throw IllegalArgumentException("가입되지 않은 번호야~")

        return ResponseEntity.ok(ApiResult.success("인증번호가 확인되었습니다."))
    }


    @Operation(summary = "비밀번호 재설정", description = "휴대폰 인증번호를 검증하고, 새 비밀번호로 변경합니다.(인증번호만 보내면 검증)")
    @PostMapping("/password/reset")
    suspend fun resetPassword(
        @Parameter(description = "비밀번호 재설정 요청 DTO") @Validated req: ResetPasswordRequest
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
            smsAuthRepository.save(data.copy(status = "VERIFIED"))

            // 🔒 비밀번호 변경 시 기존 세션 무효화
            jwtSessionService.invalidateSession(user.username)

            return ResponseEntity.ok(ApiResult.success("비밀번호가 재설정되었어~"))
        } else {
            throw IllegalArgumentException("newPassword is required")
        }

    }




    @Operation(
        summary = "비밀번호 변경 검증(로그인 상태)",
        description = "입력한 비밀번호가 현재 비밀번호와 일치한지 검증합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/password/change/valid")
    suspend fun changePasswordValid(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "비밀번호 초기화 DTO") @Validated req: PasswordCheckRequest
    ): ResponseEntity<ApiResult<Any>> {

        if (!encoder.matches(req.password, user.password)) {
            throw IllegalArgumentException("""
                비밀번호가 맞지 않아.
                다시 한번 확인 해줘
            """)
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
        @Parameter(description = "비밀번호 초기화 DTO") @Validated req: PassWordChangeRequest
    ): ResponseEntity<ApiResult<Any>> {

        if (!encoder.matches(req.oldPassword, user.password)) {
            throw IllegalArgumentException("""
                비밀번호가 맞지 않아.
                다시 한번 확인 해줘
            """)
        }

        val updated = user.copy(password = encoder.encode(req.newPassword))
        userRepository.save(updated)

        // 🔒 비밀번호 변경 시 기존 세션 무효화
        jwtSessionService.invalidateSession(user.username)

        return ResponseEntity.ok(ApiResult.success("비밀번호가 변경됐어"))
    }


    @Operation(
        summary = "연락처 변경 (로그인 상태)",
        description = "현재 비밀번호를 검증하고 새 비밀번호로 변경합니다.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @PostMapping("/phone/change")
    suspend fun changePhone(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "비밀번호 초기화 DTO") @Validated req: ChangePhoneRequest
    ): ResponseEntity<ApiResult<Any>> {

        val phone = req.phone.filter { it.isDigit() }

        // 자기 번호로 변경 시 실패 처리
        if (phone == user.phone) throw IllegalArgumentException("이미 사용 중인 번호야~")

        // 사용중인 번호인지 체크
        userRepository.findActiveByUsernameOrDeletedAtBeforeDays(phone, 30)
            ?.let { throw IllegalStateException("이미 사용중인 휴대폰 번호야") }

        // 인증번호 검증 (5분 유효)
        val minute = 5
        val data = smsAuthRepository.findValidAuthCode(phone, minute)
            ?: return ResponseEntity.badRequest().body(ApiResult.failure("인증번호가 만료되었거나 유효하지 않아~"))

        if (data.code != req.code) {
            return ResponseEntity.badRequest().body(ApiResult.failure("인증번호가 일치하지 않아~"))
        }

        // 유저 휴대폰 변경
        val updated = user.copy(username = phone, phone = phone)
        userRepository.save(updated)

        // 인증 코드 재사용 방지
        smsAuthRepository.save(data.copy(status = "VERIFIED"))

        return ResponseEntity.ok(ApiResult.success("휴대폰 번호가 변경되었어~"))

    }


    @Operation(summary = "관리자 휴대폰 인증번호 발송", description = "관리자 계정으로 등록된 휴대폰으로 인증번호를 발송합니다.")
    @PostMapping("/admin/code")
    suspend fun adminAuthCode(
        @Validated @Parameter(description = "관리자 휴대폰 인증 요청 DTO") req: AuthCodeRequest
    ): ResponseEntity<ApiResult<Any>> {
        val phone = req.phone.filter { it.isDigit() }

        // 관리자 계정 여부 확인
        val user = userRepository.findActiveByUsername(phone)
            ?: throw IllegalArgumentException("계정이 존재하지 않습니다.")

//        if (user.role != "ADMIN") {
//            throw IllegalArgumentException("관리자 권한이 없습니다.")
//        }

        val code = (100000..999999).random().toString()
        val message = "[하이틴 관리자] 인증번호는 [$code] 입니다."

        val success = smsService.sendPhone(phone, message, code)
        if (success) {
            return ResponseEntity.ok(ApiResult.success(true, "관리자 인증번호를 발송했습니다."))
        } else {
            throw IllegalStateException("인증번호 발송 실패")
        }
    }

}
