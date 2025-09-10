package kr.jiasoft.hiteen.feature.pin.app

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.pin.dto.PinRegisterRequest
import kr.jiasoft.hiteen.feature.pin.dto.PinUpdateRequest
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pins")
class PinController(
    private val pinService: PinService
) {

    /** 지도에서 볼 수 있는 핀 목록 TODO 내 주변 핀 목록 */
    @GetMapping
    suspend fun listVisiblePins(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, pinService.listVisiblePins(user)))


    /** 내가 등록한 핀 목록 */
    @GetMapping("/me")
    suspend fun myPins(
        @AuthenticationPrincipal(expression = "user") user: UserEntity
    ) = ResponseEntity.ok(ApiResult(true, pinService.listMyPins(user)))


    /** 핀 등록하기 */
    @PostMapping
    suspend fun register(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        dto: PinRegisterRequest
    ) = ResponseEntity.ok(ApiResult(true, pinService.register(user, dto)))

    /** 핀 수정 */
    @PostMapping("/update")
    suspend fun update(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        dto: PinUpdateRequest
    ) = ResponseEntity.ok(ApiResult(true, pinService.update(user, dto)))


    /** 핀 삭제 */
    @DeleteMapping("/{pinId}")
    suspend fun delete(
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
        @PathVariable pinId: Long
    ) = ResponseEntity.ok(ApiResult(true, pinService.delete(user, pinId)))


}

