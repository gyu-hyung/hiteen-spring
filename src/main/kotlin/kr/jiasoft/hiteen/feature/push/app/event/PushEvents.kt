package kr.jiasoft.hiteen.feature.push.app.event

/**
 * 도메인 로직과 무관한 사이드이펙트(FCM 발송 + push/push_detail 저장)를 비동기로 처리하기 위한 이벤트.
 *
 * payload는 가볍게 유지합니다.
 */
data class PushSendRequestedEvent(
    val userIds: List<Long>,
    val actorUserId: Long? = null,
    val templateData: Map<String, Any>,
    val extraData: Map<String, Any> = emptyMap(),
)
