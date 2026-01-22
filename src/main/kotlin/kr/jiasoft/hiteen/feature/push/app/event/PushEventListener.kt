package kr.jiasoft.hiteen.feature.push.app.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.push.app.PushService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PushEventListener(
    private val applicationCoroutineScope: CoroutineScope,
    private val pushService: PushService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * NOTE: 푸시 실패가 메인 트랜잭션/응답을 깨면 안 되므로 예외는 삼키고 로그만 남깁니다.
     * 필요하면 @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)로 바꿔 커밋 이후 실행을 보장할 수 있습니다.
     */
    @EventListener
    fun onPushSendRequested(event: PushSendRequestedEvent) {
        applicationCoroutineScope.launch {
            try {
                if (event.topic != null) {
                    pushService.sendAndSavePushToTopic(
                        topic = event.topic,
                        userId = event.actorUserId,
                        templateData = event.templateData,
                        extraData = event.extraData,
                    )
                } else if (event.userIds.isNotEmpty()) {
                    pushService.sendAndSavePush(
                        userIds = event.userIds,
                        userId = event.actorUserId,
                        templateData = event.templateData,
                        extraData = event.extraData,
                    )
                }
            } catch (e: Exception) {
                logger.warn("[PushEventListener] push send failed: {}", e.message, e)
            }
        }
    }
}
