package kr.jiasoft.hiteen.feature.soketi.app

import kr.jiasoft.hiteen.config.websocket.RedisChannelPattern
import kr.jiasoft.hiteen.feature.chat.infra.ChatUserRepository
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
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
            // 🔥 채팅방 메시지 pubsub 채널
            matches(channelName, RedisChannelPattern.CHAT_ROOM) -> {
                val roomUid = extractUuid(channelName, RedisChannelPattern.CHAT_ROOM)
                chatUsers.existsByRoomUidAndUserId(roomUid, userId)
            }

            // 🔥 채팅방 존재 여부 (presence / system 용)
            matches(channelName, RedisChannelPattern.CHAT_ROOM_MEMBERS) -> {
                val roomUid = extractUuid(channelName, RedisChannelPattern.CHAT_ROOM_MEMBERS)
                chatUsers.existsByRoomUidAndUserId(roomUid, userId)
            }

            // 🔥 친구 포함 여부 확인 (개인 push notify)
            matches(channelName, RedisChannelPattern.USER_NOTIFY) -> {
                val targetUid = extractUuid(channelName, RedisChannelPattern.USER_NOTIFY)
                val myUid = users.findUidById(userId)
                myUid == targetUid // 본인만
            }

            // 🔥 위치 공유 채널은 본인 + 친구
            matches(channelName, RedisChannelPattern.USER_LOCATION) -> {
                val targetUid = extractUuid(channelName, RedisChannelPattern.USER_LOCATION)
                val myUid = users.findUidById(userId)

                if (myUid == targetUid) {
                    true
                } else {
                    val targetId = users.findIdByUid(targetUid) ?: return false
                    friends.existsFriend(userId, targetId) > 0
                }
            }

            else -> true // 매칭 안 되는 채널은 기본 허용
        }
    }

    /** Redis 패턴과 topic이 일치하는지 판단 */
    private fun matches(channelName: String, pattern: RedisChannelPattern): Boolean {
        val prefix = pattern.pattern.substringBefore("%s")
        return channelName.startsWith(prefix)
    }

    /** 패턴 기반 topic에서 UUID 추출 */
    private fun extractUuid(channelName: String, pattern: RedisChannelPattern): UUID {
        val prefix = pattern.pattern.substringBefore("%s")
        val raw = channelName.removePrefix(prefix)
        return UUID.fromString(raw.substringBefore(":")) // members 같은 suffix 제거 가능
    }
}
