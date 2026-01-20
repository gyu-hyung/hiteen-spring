package kr.jiasoft.hiteen.feature.board.app.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class BoardEventListener(
    private val applicationCoroutineScope: CoroutineScope,
    private val followRepository: FollowRepository,
    private val userService: UserService,
    private val pushService: PushService,
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
                pushService.sendAndSavePush(
                    followerIds,
                    event.authorId,
                    PushTemplate.NEW_POST.buildPushData("nickname" to author.nickname),
                    mapOf("boardUid" to event.boardUid.toString())
                )
            } catch (e: Exception) {
                // 푸시 실패가 게시글 생성 자체를 실패시키면 안 되므로 로그만 남깁니다.
                println("⚠️ [BoardEventListener] push failed: ${e.message}")
            }
        }
    }
}
