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
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import java.util.*

@Tag(name = "User", description = "사용자 관련 API")
@SecurityRequirement(name = "bearerAuth")   // 🔑 Bearer 인증 요구
@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
) {

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
    ): ResponseEntity<ApiResult<UserResponse>> {
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

    //TODO 나만 수정가능?
    @Operation(summary = "회원정보 수정", description = "내 회원 정보를 수정합니다.")
    @PostMapping("/me/update", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Valid userUpdateForm: UserUpdateForm,
        @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<UserResponse>> =
        ResponseEntity.ok(ApiResult.success(userService.updateUser(user, userUpdateForm, file)))

    @Operation(summary = "프로필 이미지 등록", description = "여러 장의 프로필 이미지를 업로드합니다.")
    @PostMapping("/photos", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun registerImages(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @RequestPart("files", required = false) filesFlux: Flux<FilePart>?
    ) {
        val flux = filesFlux ?: filesFlux
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "files or file part is required")

        val files: List<FilePart> = flux.collectList().awaitSingle()
        userService.registerPhotos(user, files)
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
}
