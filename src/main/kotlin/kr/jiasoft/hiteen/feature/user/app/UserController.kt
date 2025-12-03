package kr.jiasoft.hiteen.feature.user.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.reactor.awaitSingle
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.ReferralSummary
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserResponseWithTokens
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import java.util.*
import kotlin.math.min

@Tag(name = "User", description = "사용자 관련 API")
@SecurityRequirement(name = "bearerAuth")   // 🔑 Bearer 인증 요구
@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
) {

    data class Users(
        val id: Long,
        val name: String,
        val avatarUrl: String,
        val status: String,
        val email: String,
        val createdAt: String,
    )

    @GetMapping("/users")
    suspend fun getUsers(
        @RequestParam("page", defaultValue = "1") pageParam: Int,
        @RequestParam("size", defaultValue = "0") sizeParam: Int,
        @RequestParam("nickname", defaultValue = "0") nickname: String? = null,
        @RequestParam("email", defaultValue = "0") email: String? = null,
        @RequestParam("phone", defaultValue = "0") phone: String? = null,
        @RequestParam("status", defaultValue = "0") status: String? = null,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<ApiPage<Users>>> {
        println("nickname = ${nickname}")
        println("email = ${email}")
        println("phone = ${phone}")
        println("status = ${status}")

        val allUsers = listOf(
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(1, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(2, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(3, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(4, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(5, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(6, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(7, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(8, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(9, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(10, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(11, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(12, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(13, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(14, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(15, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
            Users(16, "홍길동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE", "hong@naver.com", "2025-12-02"),
            Users(17, "홍이동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED", "hong@naver.com", "2025-12-02"),
            Users(18, "홍삼동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","PENDING","hong@naver.com", "2025-12-02"),
            Users(19, "홍사동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","BLOCKED","hong@naver.com", "2025-12-02"),
            Users(20, "홍오동", "https://biz.giftishow.com/Resource/brand/BR_20140528_171011_3.jpg","ACTIVE","hong@naver.com", "2025-12-02"),
        )

        // 안전한 인자값 설정
        val perPage = if (sizeParam <= 0) 10 else sizeParam
        var page = if (pageParam <= 0) 1 else pageParam

        val total = allUsers.size

        // 총 페이지 수 계산 (0이면 0)
        val lastPage = if (total == 0) 0 else ((total + perPage - 1) / perPage)

        val startIndex = (page - 1) * perPage
        val endIndex = min(total, startIndex + perPage)

        val items = if (startIndex >= total || startIndex < 0) {
            emptyList()
        } else {
            allUsers.subList(startIndex, endIndex)
        }

        val result = ApiPage(
            total = total,
            lastPage = lastPage,
            items = items,
            perPage = perPage,
            currentPage = page,
        )

        return ResponseEntity.ok(ApiResult.success(result))
    }

    @Operation(
        summary = "닉네임 중복 조회",
        description = "입력한 닉네임이 이미 사용 중인지 확인합니다.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "성공 여부 반환",
                content = [Content(schema = Schema(implementation = ApiResult::class))]
            )
        ]
    )
    @GetMapping("/nickname/{nickname}")
    suspend fun nicknameDuplicationCheck(
        @Parameter(description = "확인할 닉네임") @PathVariable nickname: String
    ): ResponseEntity<ApiResult<Boolean>> {
        val exists = userService.nicknameDuplicationCheck(nickname)
        if(exists) {
            return ResponseEntity.badRequest().body(
                ApiResult.success(exists, "이미 사용 중인 닉네임입니다.")
            )
        } else {
            return ResponseEntity.ok(
                ApiResult.success(exists, "")
            )
        }
    }

    @Operation(summary = "회원가입", description = "신규 회원을 등록합니다.")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun register(
        @Valid userRegisterForm: UserRegisterForm,
        @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<UserResponseWithTokens>> {
        val user = userService.register(userRegisterForm, file)
        return ResponseEntity.ok(ApiResult.success(user))
    }

    @Operation(summary = "내 정보 조회", description = "현재 로그인한 회원 정보를 조회합니다.")
    @GetMapping("/me")
    suspend fun me(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<UserResponse>> {
        return ResponseEntity.ok(ApiResult.success(userService.findUserResponse(user.uid)))
    }

    @Operation(summary = "회원 프로필 조회", description = "특정 회원의 프로필 정보를 조회합니다.")
    @GetMapping("/profile/{uid}")
    suspend fun profile(
        @Parameter(description = "조회할 사용자 UID") @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") currentUser: UserEntity?
    ): ResponseEntity<ApiResult<UserResponse>> {
        return ResponseEntity.ok(
            ApiResult.success(
                userService.findUserResponse(uid, currentUser?.id)
            )
        )
    }


    @Operation(summary = "회원 프로필 조회", description = "특정 회원의 프로필 정보를 조회합니다.")
    @GetMapping("/profile/s/{id}")
    suspend fun profile(
        @Parameter(description = "조회할 사용자 UID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") currentUser: UserEntity?
    ): ResponseEntity<ApiResult<UserSummary>> {
        return ResponseEntity.ok(
            ApiResult.success(
                userService.findUserSummary(id)
            )
        )
    }


    @Operation(summary = "회원 프로필 조회", description = "특정 회원의 프로필 정보를 조회합니다.")
    @GetMapping("/profile/ss/{id}")
    suspend fun profiless(
        @Parameter(description = "조회할 사용자 UID") @PathVariable id: Long,
        @AuthenticationPrincipal(expression = "user") currentUser: UserEntity?
    ): ResponseEntity<ApiResult<UserEntity>> {
        return ResponseEntity.ok(
            ApiResult.success(
                userService.findByUsername(id)
            )
        )
    }


    //TODO 나만 수정가능?
    @Operation(summary = "회원정보 수정", description = "내 회원 정보를 수정합니다.")
    @PostMapping("/me/update", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Valid userUpdateForm: UserUpdateForm,
        @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<UserResponse>> =
        ResponseEntity.ok(ApiResult.success(userService.updateUser(user, userUpdateForm, file)))


    @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자를 탈퇴 처리합니다.")
    @DeleteMapping("/withdraw")
    suspend fun withdraw(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<String>> {
        userService.withdraw(user)
        return ResponseEntity.ok(ApiResult.success("회원 탈퇴가 완료되었습니다."))
    }

    @Operation(summary = "프로필 이미지 등록", description = "여러 장의 프로필 이미지를 업로드합니다.")
    @PostMapping("/photos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun registerImages(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart("files", required = false) filesFlux: Flux<FilePart>
    ): ResponseEntity<ApiResult<UserResponse>> {
        val files: List<FilePart> = filesFlux.collectList().awaitSingle()
        val userResponse = userService.registerPhotos(user, files)
        return ResponseEntity.ok(ApiResult.success(userResponse))
    }

    @Operation(summary = "사진 삭제", description = "사용자의 특정 사진을 삭제합니다.")
    @DeleteMapping("/photos/{photoId}")
    suspend fun deletePhoto(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Parameter(description = "삭제할 사진 ID") @PathVariable photoId: Long
    ) = ResponseEntity.ok(ApiResult.success(userService.deletePhoto(user, photoId)))

    @Operation(summary = "사진 조회", description = "특정 회원의 사진 목록을 조회합니다.")
    @GetMapping("/photos")
    suspend fun list(
        @Parameter(description = "조회할 사용자 UID") @RequestParam(required = true) userUid: String
    ) = ResponseEntity.ok(ApiResult.success(userService.getPhotos(userUid)))


    @Operation(summary = "나를 추천인으로 등록한 친구 조회")
    @GetMapping("/referral")
    suspend fun referral(@AuthenticationPrincipal(expression = "user") user: UserEntity)
        : ResponseEntity<ApiResult<List<ReferralSummary>>>
        = ResponseEntity.ok(ApiResult.success(userService.myReferralList(user.id)))


}
