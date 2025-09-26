package kr.jiasoft.hiteen.feature.soketi.app

import kr.jiasoft.hiteen.feature.chat.infra.ChatUserRepository
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.soketi.domain.SoketiChannelPattern
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

interface ChannelAuthorizationService {
    suspend fun canSubscribe(userId: Long?, channelName: String): Boolean
}


@Service
class DefaultChannelAuthorizationService(
    private val chatUsers: ChatUserRepository,
    private val users: UserRepository,
    private val friends: FriendRepository,
) : ChannelAuthorizationService {

    override suspend fun canSubscribe(userId: Long?, channelName: String): Boolean {
        if (userId == null) return false

        return when {
            channelName.startsWith(SoketiChannelPattern.PRIVATE_CHAT_ROOM.pattern) -> {
                val roomUid = UUID.fromString(
                    channelName.removePrefix(SoketiChannelPattern.PRIVATE_CHAT_ROOM.pattern)
                )
                chatUsers.existsByRoomUidAndUserId(roomUid, userId)
            }

            channelName.startsWith(SoketiChannelPattern.PRIVATE_USER.pattern) -> {
                val targetUserUid = UUID.fromString(
                    channelName.removePrefix(SoketiChannelPattern.PRIVATE_USER.pattern)
                )
                val myUid = users.findUidById(userId)
                myUid == targetUserUid
            }

            channelName.startsWith(SoketiChannelPattern.PRIVATE_USER_LOCATION.pattern) -> {
                val targetUserUid = UUID.fromString(
                    channelName.removePrefix(SoketiChannelPattern.PRIVATE_USER_LOCATION.pattern)
                )
                val myUid = users.findUidById(userId)

                if (myUid == targetUserUid) {
                    true // 본인
                } else {
                    val targetId = users.findIdByUid(targetUserUid) ?: return false
                    friends.existsFriend(userId, targetId) > 0
                }
            }

            else -> true
        }
    }

}