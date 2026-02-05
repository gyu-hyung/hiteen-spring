package kr.jiasoft.hiteen.feature.notification.app

import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.notification.dto.PushNotificationResponse
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.PushTemplateGroup
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
        code: PushTemplate?,
        group: PushTemplateGroup?,
    ): ApiPageCursor<PushNotificationResponse> {

        val entities = when {
            code != null -> pushDetailRepository.findByUserIdWithCursor(userId, cursor, limit, code.code)
            group != null -> {
                val codes = PushTemplate.entries
                    .filter { it.group == group }
                    .map { it.code }
                    .toTypedArray()
                pushDetailRepository.findByUserIdWithCursorAndCodes(userId, cursor, limit, codes)
            }
            else -> pushDetailRepository.findByUserIdWithCursor(userId, cursor, limit, null)
        }

        val items = entities.map {
            PushNotificationResponse(
                id = it.id,
                code = it.code,
                title = it.title,
                message = it.message,
                success = it.success,
                nickname = it.nickname,
                assetUid = it.assetUid,
                targetType = it.targetType,
                targetId = it.targetId,
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


     suspend fun delete(userId: Long, pushId: Long? = null, all: Boolean) {
        if(all) {
            pushDetailRepository.softDeleteByUserId(userId)
        } else {
            requireNotNull(pushId)
            pushDetailRepository.softDeleteByIdAndUserId(pushId, userId)
        }
    }

}
