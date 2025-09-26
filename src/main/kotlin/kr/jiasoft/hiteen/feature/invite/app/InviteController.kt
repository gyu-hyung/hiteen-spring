package kr.jiasoft.hiteen.feature.invite.app

import io.swagger.v3.oas.annotations.Parameter
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/api/invite")
class InviteController (
    private val inviteService: InviteService
){



    @PostMapping("/{targetUid}")
    suspend fun createInvite(@AuthenticationPrincipal(expression = "user") user: UserEntity, @Parameter(description = "초대 대상 UUID") @PathVariable targetUid: UUID) {
        ResponseEntity.ok(ApiResult.success(inviteService.giveInviteExp(user.id, targetUid)))
    }


}