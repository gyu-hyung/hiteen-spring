package kr.jiasoft.hiteen.feature.user.app

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
import java.util.UUID

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
) {

    /** 닉네임 중복 조회 */
    @GetMapping("/nickname/{nickname}")
    suspend fun nicknameDuplicationCheck(@PathVariable nickname: String): ResponseEntity<ApiResult<Boolean>> {
        val exists = userService.nicknameDuplicationCheck(nickname)
        return ResponseEntity.ok(
            ApiResult(
                success = true,
                data = exists,
                message = if (exists) "이미 사용 중인 닉네임입니다." else "사용 가능한 닉네임입니다."
            )
        )
    }

    /** 회원가입
     * TODO : 학년 ex) 3 -> 고3
     * */
    @PostMapping
    suspend fun register(
        @Valid userRegisterForm: UserRegisterForm,
        @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<UserResponse>> {
        val user = userService.register(userRegisterForm, file)
        return ResponseEntity.ok(ApiResult(success = true, data = user, message = "회원가입 완료"))
    }

    /** 회원정보 조회 */
    @GetMapping("/me")
    suspend fun me(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ): ResponseEntity<ApiResult<UserResponse>> {
        return ResponseEntity.ok(ApiResult.success(userService.findUserResponse(user.uid)))
    }


    /** 회원 프로필 조회 */
    @GetMapping("/profile/{uid}")
    suspend fun profile(
        @PathVariable uid: UUID,
        @AuthenticationPrincipal(expression = "user") currentUser: UserEntity?
    ): ResponseEntity<ApiResult<UserResponse>> {
        return ResponseEntity.ok(
            ApiResult.success(
                userService.findUserResponse(uid, currentUser?.id)
            )
        )
    }



    /** 회원정보 수정 */
    @PostMapping("/me/update")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @Valid userUpdateForm: UserUpdateForm,
        @RequestPart("file", required = false) file: FilePart?
    ): ResponseEntity<ApiResult<UserResponse>>
        = ResponseEntity.ok(ApiResult.success(userService.updateUser(user, userUpdateForm, file)))


    /** 회원정보 프로필 이미지 등록 */
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

    /** 사용자 사진 삭제 */
    @PostMapping("/photos/delete/{photoId}")
    suspend fun deletePhoto(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable photoId: Long
    ) = ResponseEntity.ok(ApiResult.success(userService.deletePhoto(user, photoId)))


    /** 사용자 사진 조회 */
    @GetMapping("/photos")
    suspend fun list(@RequestParam(required = true) userUid: String)
            = ResponseEntity.ok(ApiResult.success(userService.getPhotos(userUid)))

}
