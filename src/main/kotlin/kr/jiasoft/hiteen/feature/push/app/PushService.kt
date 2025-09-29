package kr.jiasoft.hiteen.feature.push.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.push.domain.PushDetailEntity
import kr.jiasoft.hiteen.feature.push.infra.PushDetailRepository
import kr.jiasoft.hiteen.feature.user.infra.UserDetailRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PushService(
    private val firebaseMessaging: FirebaseMessaging,
    private val userDetailRepository: UserDetailRepository,
    private val pushDetailRepository: PushDetailRepository
) {
    suspend fun sendPush(
        pushId: Long?,
        deviceOs: String,
        userIds: List<Long>,
        data: Map<String, Any>,
        isSilent: Boolean = false
    ) {
        if (userIds.isEmpty()) return

        val userDetails = userDetailRepository.findUsersWithDetail(userIds)
            .filter { it.deviceToken != null && it.deviceOs == deviceOs }

        val tokens = userDetails.mapNotNull { it.deviceToken }.distinct()
        if (tokens.isEmpty()) return

        val tokenChunks = tokens.chunked(500) // 권장: 500개 단위

        for (chunk in tokenChunks) {
            val multicastMessage = buildMessage(deviceOs, data, chunk, isSilent)

            withContext(Dispatchers.IO) {
                val response = firebaseMessaging.sendEachForMulticast(multicastMessage)

                response.responses.forEachIndexed { idx, sendResponse ->
                    val token = chunk[idx]
                    val userDetail = userDetails.firstOrNull { it.deviceToken == token }

                    val entity = PushDetailEntity(
                        pushId = pushId ?: 0L,
                        userId = userDetail?.userId,
                        deviceOs = userDetail?.deviceOs,
                        deviceToken = token,
                        phone = userDetail?.phone,
                        multicastId = null,
                        messageId = if (sendResponse.isSuccessful) sendResponse.messageId else null,
                        error = sendResponse.exception?.message,
                        success = sendResponse.isSuccessful,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now()
                    )
                    pushDetailRepository.save(entity)
                }

                println("🔥 Firebase sendEachForMulticast => success=${response.successCount}, failure=${response.failureCount}")
            }
        }
    }

    private fun buildMessage(
        deviceOs: String,
        data: Map<String, Any>,
        tokens: List<String>,
        isSilent: Boolean
    ): MulticastMessage {
        val messageData = data.mapValues { (_, v) ->
            when (v) {
                is Collection<*> -> jacksonObjectMapper().writeValueAsString(v)
                else -> v.toString()
            }
        }.toMutableMap()

        if (messageData["message"].isNullOrBlank()) {
            messageData["message"] = "메시지 없음"
        }
        if (messageData["title"].isNullOrBlank()) {
            messageData["title"] = messageData["message"]!!
        }

        val notification = Notification.builder()
            .setTitle(messageData["title"])
            .setBody(messageData["message"])
            .build()

        val baseMessage = MulticastMessage.builder()
            .addAllTokens(tokens)
            .putAllData(messageData)

        return when (deviceOs) {
            "Android" -> baseMessage
                .setNotification(notification)
                .setAndroidConfig(
                    AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build()
                )
                .build()

            "iOS" -> {
                if (isSilent) {
                    baseMessage.setApnsConfig(
                        ApnsConfig.builder()
                            .putHeader("apns-push-type", "background")
                            .putHeader("apns-priority", "5")
                            .setAps(Aps.builder().setContentAvailable(true).build())
                            .build()
                    ).build()
                } else {
                    baseMessage.setNotification(notification)
                        .setApnsConfig(
                            ApnsConfig.builder()
                                .setAps(Aps.builder().setSound("default").build())
                                .build()
                        )
                        .build()
                }
            }

            else -> baseMessage.setNotification(notification).build()
        }
    }
}
