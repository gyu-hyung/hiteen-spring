package kr.jiasoft.hiteen.feature.push.app

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.messaging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.push.domain.PushDetailEntity
import kr.jiasoft.hiteen.feature.push.domain.PushEntity
import kr.jiasoft.hiteen.feature.push.infra.PushDetailRepository
import kr.jiasoft.hiteen.feature.push.infra.PushRepository
import kr.jiasoft.hiteen.feature.user.infra.UserDetailRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PushService(
    private val firebaseMessaging: FirebaseMessaging,
    private val userDetailRepository: UserDetailRepository,
    private val pushRepository: PushRepository,
    private val pushDetailRepository: PushDetailRepository
) {

    /**
     * 전체 푸시 전송 + 요약 저장 + 상세 기록
     */
    suspend fun sendAndSavePush(
        deviceOs: String,
        userIds: List<Long>,
        data: Map<String, Any>,
        isSilent: Boolean = false
    ): SendResult {
        if (userIds.isEmpty()) return SendResult(0, 0, 0)

        // ① push 요약 저장
        val push = pushRepository.save(
            PushEntity(
                type = if (isSilent) "silent" else "notification",
                code = data["code"]?.toString(),
                title = data["title"]?.toString(),
                message = data["message"]?.toString(),
                total = userIds.size.toLong()
            )
        )

        // ② 실제 FCM 전송
        val result = sendPush(push.id, deviceOs, userIds, data, isSilent)

        // ③ 요약 테이블에 성공/실패 반영
        val updated = push.copy(
            success = result.success.toLong(),
            failure = result.failure.toLong(),
            updatedAt = OffsetDateTime.now()
        )
        pushRepository.save(updated)

        println("✅ [PushService] pushId=${push.id}, sent all batches, success=${result.success}, failure=${result.failure}")
        return SendResult(pushId = push.id, success = result.success, failure = result.failure)
    }

    /**
     * 실제 Firebase에 전송하고, push_detail 저장
     */
    suspend fun sendPush(
        pushId: Long?,
        deviceOs: String,
        userIds: List<Long>,
        data: Map<String, Any>,
        isSilent: Boolean
    ): SendResult {
        if (userIds.isEmpty()) return SendResult(0, 0, 0)

        // 사용자 상세 + 토큰 필터링
        val userDetails = userDetailRepository.findUsersWithDetail(userIds)
            .filter { it.deviceToken != null && it.deviceOs == deviceOs }

        val tokens = userDetails.mapNotNull { it.deviceToken }.distinct()
        if (tokens.isEmpty()) return SendResult(pushId ?: 0L, 0, 0)

        var totalSuccess = 0
        var totalFailure = 0

        // chunk 단위로 묶음 전송
        val chunks = tokens.chunked(500)
        for (chunk in chunks) {
            val message = buildMessage(deviceOs, data, chunk, isSilent)

            withContext(Dispatchers.IO) {
                try {
                    val response = firebaseMessaging.sendEachForMulticast(message)
                    totalSuccess += response.successCount
                    totalFailure += response.failureCount

                    response.responses.forEachIndexed { idx, sendResponse ->
                        val token = chunk[idx]
                        val userDetail = userDetails.firstOrNull { it.deviceToken == token }

                        val detail = PushDetailEntity(
                            pushId = pushId,
                            userId = userDetail?.userId,
                            deviceOs = userDetail?.deviceOs,
                            deviceToken = token,
                            phone = userDetail?.phone,
                            multicastId = null,
                            messageId = if (sendResponse.isSuccessful) sendResponse.messageId else null,
                            error = sendResponse.exception?.message,
                            success = if (sendResponse.isSuccessful) 1 else 0,
                            createdAt = OffsetDateTime.now(),
                            updatedAt = OffsetDateTime.now()
                        )
                        pushDetailRepository.save(detail)
                    }

                    println("🔥 Firebase sendEachForMulticast → success=${response.successCount}, failure=${response.failureCount}")
                } catch (ex: Exception) {
                    // 전송 오류의 경우, chunk 전체에 대해 실패 처리
                    chunk.forEach { token ->
                        val userDetail = userDetails.firstOrNull { it.deviceToken == token }
                        val detail = PushDetailEntity(
                            pushId = pushId,
                            userId = userDetail?.userId,
                            deviceOs = userDetail?.deviceOs,
                            deviceToken = token,
                            phone = userDetail?.phone,
                            multicastId = null,
                            messageId = null,
                            error = ex.message,
                            success = 0,
                            createdAt = OffsetDateTime.now(),
                            updatedAt = OffsetDateTime.now()
                        )
                        pushDetailRepository.save(detail)
                    }
                    println("‼️ Firebase sendEachForMulticast exception: ${ex.message}")
                    // 실패 횟수엔 chunk 전체 수 반영
                    totalFailure += chunk.size
                }
            }
        }

        return SendResult(pushId = pushId ?: 0L, success = totalSuccess, failure = totalFailure)
    }

    /**
     * 메시지 객체 구성
     * - notification + data 구조 분리
     * - Android / iOS 옵션 강화
     */
    private fun buildMessage(
        deviceOs: String,
        data: Map<String, Any>,
        tokens: List<String>,
        isSilent: Boolean
    ): MulticastMessage {
        val messageData = data.mapValues { it.value.toString() }

        val title = messageData["title"] ?: "알림"
        val body = messageData["message"] ?: "내용 없음"

        val notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .build()

        return MulticastMessage.builder()
            .addAllTokens(tokens)
            .setNotification(notification)
            .putAllData(messageData)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(
                        AndroidNotification.builder()
                            .setChannelId("default_channel")
                            .setSound("default")
                            .build()
                    )
                    .build()
            )
            .build()
    }


    data class SendResult(
        val pushId: Long,
        val success: Int,
        val failure: Int
    )
}
