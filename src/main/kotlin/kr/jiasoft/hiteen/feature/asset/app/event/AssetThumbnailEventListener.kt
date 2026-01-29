package kr.jiasoft.hiteen.feature.asset.app.event

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AssetThumbnailEventListener(
    private val applicationCoroutineScope: CoroutineScope,
    private val assetService: AssetService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * NOTE:
     * - 썸네일 생성 실패가 메인 응답/트랜잭션을 깨면 안 되므로 예외는 삼키고 로그만 남깁니다.
     * - 필요하면 @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)로 바꿔
     *   DB 커밋 이후 실행을 보장할 수 있습니다.
     */
    @EventListener
    fun onPrecreateRequested(event: AssetThumbnailPrecreateRequestedEvent) {
        if (event.assetUids.isEmpty()) return

        applicationCoroutineScope.launch {
            event.assetUids.forEach { uid ->
                try {
                    assetService.getOrCreateThumbnail(
                        uid = uid,
                        width = event.width,
                        height = event.height,
                        currentUserId = event.requestedByUserId,
                        mode = event.mode,
                    )
                } catch (e: Exception) {
                    log.warn(
                        "[AssetThumbnailEventListener] precreate failed: uid={} size={}x{} err={}",
                        uid,
                        event.width,
                        event.height,
                        e.message,
                        e
                    )
                }
            }
        }
    }
}

