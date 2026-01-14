package kr.jiasoft.hiteen.feature.chat.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.chat.domain.ChatRoomInviteMode
import java.util.UUID

@Schema(description = "채팅방 수정 요청")
data class UpdateRoomRequest(
    @param:Schema(description = "채팅방 이름(room_name)", example = "우리반 단톡")
    val roomName: String? = null,

    @param:Schema(description = "채팅방 친구 초대 권한(invite_mode)", example = "ALL_MEMBERS")
    val inviteMode: ChatRoomInviteMode? = null,

    @param:Schema(description = "채팅방 썸네일 asset uid(asset_uid)", example = "f580e8e8-adee-4285-b181-3fed545e7be0")
    val assetUid: UUID? = null,
)

