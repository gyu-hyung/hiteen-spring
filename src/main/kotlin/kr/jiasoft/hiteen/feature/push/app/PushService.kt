package kr.jiasoft.hiteen.feature.push.app

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.messaging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.jiasoft.hiteen.feature.push.domain.PushDetailEntity
import kr.jiasoft.hiteen.feature.push.domain.PushEntity
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.infra.PushDetailRepository
import kr.jiasoft.hiteen.feature.push.infra.PushRepository
import kr.jiasoft.hiteen.feature.user.domain.PushItemType
import kr.jiasoft.hiteen.feature.user.infra.UserDetailRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class PushService(
    private val firebaseMessaging: FirebaseMessaging,
    private val userDetailRepository: UserDetailRepository,
    private val pushRepository: PushRepository,
    private val pushDetailRepository: PushDetailRepository,
    private val objectMapper: ObjectMapper
) {

    /**
     * 전체 푸시 전송 + 요약 저장 + 상세 기록
     */
    suspend fun sendAndSavePush(
        userIds: List<Long>,
        userId: Long? = null,
        templateData: Map<String, Any>,
        extraData: Map<String, Any> = emptyMap(),
    ): SendResult {
        if (userIds.isEmpty()) return SendResult(0, 0, 0)

        val finalData = templateData + extraData

        // ① push 요약 저장
        val push = pushRepository.save(
            PushEntity(
                type = if (finalData["silent"] == true) "silent" else "notification",
                code = finalData["code"]?.toString(),
                title = finalData["title"]?.toString(),
                message = finalData["message"]?.toString(),
                total = userIds.size.toLong(),
                createdId = userId,
            )
        )

        // ② 실제 FCM 전송
        val result = sendPush(push.id, userIds, finalData)

        // ③ 요약 업데이트
        if(result.success + result.failure > 0) {
            pushRepository.save(
                push.copy(
                    success = result.success.toLong(),
                    failure = result.failure.toLong(),
                    updatedAt = OffsetDateTime.now()
                )
            )
        }

        println("✅ [PushService] pushId=${push.id}, success=${result.success}, failure=${result.failure}")
        return result
    }

    /**
     * 실제 Firebase에 전송하고, push_detail 저장
     */
    private suspend fun sendPush(
        pushId: Long,
        userIds: List<Long>,
        data: Map<String, Any>
    ): SendResult {
        val code = data["code"]?.toString() ?: return SendResult(pushId, 0, 0)

        // ① 유저 상세 정보 조회 (deviceToken이 존재하는 사용자만)
        val userDetails = userDetailRepository.findUsersWithDetail(userIds)
            .filter { !it.deviceToken.isNullOrBlank() }
            .filter { it.pushService == "Y" }

        val template = PushTemplate.entries.firstOrNull { it.code == code }
        // ② pushItems 허용 여부 확인
        val eligibleUsers = userDetails.filter { detail ->
            try {
                // pushItems 없는 경우 → 모두 허용
                if (template?.itemType == null) return@filter true

                val pushList: List<String> =
                    objectMapper.readValue(detail.pushItems ?: "[]", object : TypeReference<List<String>>() {})

                pushList.contains(PushItemType.ALL.name) || pushList.contains(template.itemType.name)
            } catch (e: Exception) {
                println("⚠️ pushItems 파싱 실패 (userId=${detail.userId}): ${e.message}")
                false
            }
        }

        if (eligibleUsers.isEmpty()) {
            println("⚠️ [PushService] '$code' 푸시를 허용한 사용자가 없습니다.")
            return SendResult(pushId, 0, 0)
        }

        // ③ 실제 전송 대상 토큰 목록 구성
        val tokens = eligibleUsers.mapNotNull { it.deviceToken }.distinct()
        if (tokens.isEmpty()) return SendResult(pushId, 0, 0)

        var totalSuccess = 0
        var totalFailure = 0

        val chunks = tokens.chunked(500)
        for (chunk in chunks) {
            val message = buildMessage(data, chunk)

            withContext(Dispatchers.IO) {
                try {
                    val response = firebaseMessaging.sendEachForMulticast(message)
                    totalSuccess += response.successCount
                    totalFailure += response.failureCount

                    response.responses.forEachIndexed { idx, res ->
                        val token = chunk[idx]
                        val userDetail = eligibleUsers.firstOrNull { it.deviceToken == token }

                        pushDetailRepository.save(
                            PushDetailEntity(
                                pushId = pushId,
                                userId = userDetail?.userId,
                                deviceOs = userDetail?.deviceOs,
                                deviceToken = token,
                                phone = userDetail?.phone,
                                messageId = if (res.isSuccessful) res.messageId else null,
                                error = res.exception?.message,
                                success = if (res.isSuccessful) 1 else 0,
                                createdAt = OffsetDateTime.now(),
                                updatedAt = OffsetDateTime.now()
                            )
                        )
                    }

                    println("🔥 Firebase sendEachForMulticast success=${response.successCount}, failure=${response.failureCount}")
                } catch (ex: Exception) {
                    totalFailure += chunk.size
                    chunk.forEach { token ->
                        val userDetail = eligibleUsers.firstOrNull { it.deviceToken == token }
                        pushDetailRepository.save(
                            PushDetailEntity(
                                pushId = pushId,
                                userId = userDetail?.userId,
                                deviceOs = userDetail?.deviceOs,
                                deviceToken = token,
                                phone = userDetail?.phone,
                                messageId = null,
                                error = ex.message,
                                success = 0,
                                createdAt = OffsetDateTime.now(),
                                updatedAt = OffsetDateTime.now()
                            )
                        )
                    }
                    println("‼️ Firebase sendEachForMulticast exception: ${ex.message}")
                }
            }
        }

        return SendResult(pushId, totalSuccess, totalFailure)
    }


    /**
     * 메시지 객체 구성
     * - notification + data 구조 분리
     * - silent 여부 자동 판단
     */
    private fun buildMessage(data: Map<String, Any>, tokens: List<String>): MulticastMessage {
        val messageData = data.mapValues { it.value.toString() }
        val isSilent = messageData["silent"]?.toBoolean() == true

        val builder = MulticastMessage.builder().addAllTokens(tokens).putAllData(messageData)

        if (!isSilent) {
            val notification = Notification.builder()
                .setTitle(messageData["title"] ?: "알림")
                .setBody(messageData["message"] ?: "")
                .build()

            builder.setNotification(notification)
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
        }

        return builder.build()
    }

    data class SendResult(
        val pushId: Long,
        val success: Int,
        val failure: Int
    )
}
