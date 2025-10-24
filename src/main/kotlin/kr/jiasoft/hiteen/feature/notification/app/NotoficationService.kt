package kr.jiasoft.hiteen.feature.notification.app

import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.notification.dto.PushNotificationResponse
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.infra.PushDetailRepository
import org.springframework.stereotype.Service

@Service
class NotificationService(
    private val pushDetailRepository: PushDetailRepository
) {

    suspend fun getPushNotifications(
        userId: Long,
        cursor: Long?,    // 마지막으로 조회된 push_detail.id
        limit: Int,       // 페이지당 개수
        code: PushTemplate?
    ): ApiPageCursor<PushNotificationResponse> {

        val entities = pushDetailRepository.findByUserIdWithCursor(userId, cursor, limit, code?.code)
        val items = entities.map {
            PushNotificationResponse(
                id = it.id,
                code = it.code,
                title = it.title,
                message = it.message,
                success = it.success,
                createdAt = it.createdAt
            )
        }

        // 다음 커서 계산 (가장 마지막 id)
        val nextCursor = items.lastOrNull()?.id?.toString()

        return ApiPageCursor(
            nextCursor = nextCursor,
            items = items,
            perPage = limit
        )
    }


}
