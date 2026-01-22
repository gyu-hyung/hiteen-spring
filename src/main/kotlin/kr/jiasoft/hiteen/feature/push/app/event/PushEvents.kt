package kr.jiasoft.hiteen.feature.push.app.event

import kr.jiasoft.hiteen.feature.user.domain.PushItemType

/**
 * 도메인 로직과 무관한 사이드이펙트(FCM 발송 + push/push_detail 저장)를 비동기로 처리하기 위한 이벤트.
 *
 */
data class PushSendRequestedEvent(
    val userIds: List<Long> = emptyList(),
    val topic: PushItemType? = null,
    val actorUserId: Long? = null,
    val templateData: Map<String, Any>,
    val extraData: Map<String, Any> = emptyMap(),
)
