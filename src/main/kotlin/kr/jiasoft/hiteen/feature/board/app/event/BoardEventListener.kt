package kr.jiasoft.hiteen.feature.board.app.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 이 리스너는 더 이상 사용되지 않습니다(BoardService에서 직접 PushSendRequestedEvent 발행).
 * @Component 제거
 */
class BoardEventListener(
    private val applicationCoroutineScope: CoroutineScope,
    private val followRepository: FollowRepository,
    private val userService: UserService,
    private val eventPublisher: ApplicationEventPublisher,
) {

    /**
     * 게시글 생성 후 푸시 발송.
     *
     * NOTE: 이 리스너는 비동기로 실행되며, API 응답을 블로킹하지 않습니다.
     * 트랜잭션 커밋 이후 실행이 필요하면 @TransactionalEventListener 로 변경하세요.
     */
    @EventListener
    fun onBoardCreated(event: BoardCreatedEvent) {
        applicationCoroutineScope.launch {
            try {
                val followerIds = followRepository.findAllFollowerIds(event.authorId).toList()
                if (followerIds.isEmpty()) return@launch

                val author = userService.findUserSummary(event.authorId)
                eventPublisher.publishEvent(
                    PushSendRequestedEvent(
                        userIds = followerIds,
                        actorUserId = event.authorId,
                        templateData = PushTemplate.NEW_POST.buildPushData("nickname" to author.nickname),
                        extraData = mapOf("boardUid" to event.boardUid.toString()),
                    )
                )
            } catch (e: Exception) {
                // 푸시 실패가 게시글 생성 자체를 실패시키면 안 되므로 로그만 남깁니다.
                println("⚠️ [BoardEventListener] publish push event failed: ${e.message}")
            }
        }
    }
}
