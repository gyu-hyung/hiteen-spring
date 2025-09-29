package kr.jiasoft.hiteen.feature.notification.app

import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
import kr.jiasoft.hiteen.feature.chat.infra.ChatMessageRepository
import kr.jiasoft.hiteen.feature.push.infra.PushRepository
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val pushRepository: PushRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val boardRepository: BoardRepository,
//    private val giftRepository: GiftRepository,
    private val friendRepository: FriendRepository
) {

//    suspend fun getAlerts(userId: Long): NotificationResponse {
//        val push = pushRepository.findLatestByUserId(userId)?.let {
//            NotificationItem(
//                uid = it.seq?.let { seq -> UUID.nameUUIDFromBytes(seq.toString().toByteArray()) },
//                message = it.message,
//                createdAt = it.createdAt
//            )
//        }
//
//        val chat = chatMessageRepository.findLatestByUserId(userId)?.let {
//            NotificationItem(
//                uid = it.chatUid,
//                message = it.message,
//                createdAt = it.createdAt
//            )
//        }
//
//        val notice = boardRepository.findLatestNotice()?.let {
//            NotificationItem(
//                uid = it.uid,
//                message = it.subject,
//                createdAt = it.createdAt
//            )
//        }
//
//        val event = boardRepository.findLatestEvent()?.let {
//            NotificationItem(
//                uid = it.uid,
//                message = it.subject,
//                createdAt = it.createdAt
//            )
//        }
//
////        val gift = giftRepository.findLatestReceivedGift(userId)?.let {
////            NotificationItem(
////                uid = it.uid,
////                message = it.memo ?: "받은 선물",
////                createdAt = it.createdAt
////            )
////        }
//
//        val friend = friendRepository.findLatestFriendRequest(userId)?.let {
//            NotificationItem(
//                uid = it.userUid,
//                message = "${it.userName}, 나에게 친구를 요청했어~",
//                createdAt = it.statusDate
//            )
//        }
//
//        return NotificationResponse(push, chat, notice, event, gift, friend)
//    }
}
