package kr.jiasoft.hiteen.feature.invite.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "InviteDeferredIssueResponse")
data class InviteDeferredIssueResponse(
    val token: String,
)

@Schema(name = "InviteDeferredResolveResponse")
data class InviteDeferredResolveResponse(
    val code: String,
)

