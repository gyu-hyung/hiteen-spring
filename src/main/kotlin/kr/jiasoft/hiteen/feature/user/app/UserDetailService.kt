package kr.jiasoft.hiteen.feature.user.app

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.jiasoft.hiteen.feature.user.domain.UserDetailEntity
import kr.jiasoft.hiteen.feature.user.dto.UserDetailRequest
import kr.jiasoft.hiteen.feature.user.dto.UserDetailResponse
import kr.jiasoft.hiteen.feature.user.infra.UserDetailRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.collections.joinToString

@Service
class UserDetailService(
    private val userDetails: UserDetailRepository,
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
) {

    suspend fun getUserDetail(userUid: UUID): UserDetailResponse? {
        val entity = userDetails.findByUid(userUid) ?: return null
        return entity.toResponse()
    }

    suspend fun upsertUserDetail(userUid: UUID, req: UserDetailRequest): UserDetailResponse {
        userRepository.findIdByUid(userUid)?.let {
            req.userId = it
        }

        // 1️⃣ pushItems 안전 변환
        val validPushItems = req.pushItems
            ?.distinct()                 // 중복 제거
            .takeIf { it?.isNotEmpty() == true} // 비어있지 않으면 유지
            ?: emptyList()               // null 또는 빈 경우 기본값

        // 2️⃣ JSON 안전 직렬화
        val pushItemsJson = try {
            objectMapper.writeValueAsString(validPushItems)
        } catch (e: Exception) {
            "[]" // 실패 시 안전한 기본값
        }

        val existing = userDetails.findByUid(userUid)
        val saved = existing?.copy(
            deviceId = req.deviceId ?: existing.deviceId,
            deviceOs = req.deviceOs ?: existing.deviceOs,
            deviceVersion = req.deviceVersion ?: existing.deviceVersion,
            deviceDetail = req.deviceDetail ?: existing.deviceDetail,
            deviceToken = req.deviceToken ?: existing.deviceToken,
            locationToken = req.locationToken ?: existing.locationToken,
            aqnsToken = req.aqnsToken ?: existing.aqnsToken,
            apiToken = req.apiToken ?: existing.apiToken,
            agreeService = req.agreeService ?: existing.agreeService,
            agreePrivacy = req.agreePrivacy ?: existing.agreePrivacy,
            agreeFinance = req.agreeFinance ?: existing.agreeFinance,
            agreeMarketing = req.agreeMarketing ?: existing.agreeMarketing,
            pushService = req.pushService ?: existing.pushService,
            pushMarketing = req.pushMarketing ?: existing.pushMarketing,
            pushItems = pushItemsJson,
            memo = req.memo ?: existing.memo,
        ) ?: UserDetailEntity(
            userId = req.userId!!,
            deviceId = req.deviceId,
            deviceOs = req.deviceOs,
            deviceVersion = req.deviceVersion,
            deviceDetail = req.deviceDetail,
            deviceToken = req.deviceToken,
            locationToken = req.locationToken,
            aqnsToken = req.aqnsToken,
            apiToken = req.apiToken,
            agreeService = req.agreeService,
            agreePrivacy = req.agreePrivacy,
            agreeFinance = req.agreeFinance,
            agreeMarketing = req.agreeMarketing,
            pushService = req.pushService,
            pushMarketing = req.pushMarketing,
            pushItems = pushItemsJson,
            memo = req.memo,
        )

        return userDetails.save(saved).toResponse()
    }


    suspend fun deleteUserDetail(userUid: UUID) {
        userDetails.deleteByUid(userUid)
    }

    private fun UserDetailEntity.toResponse() = UserDetailResponse(
        userId = this.userId,
        deviceId = this.deviceId,
        deviceOs = this.deviceOs,
        deviceVersion = this.deviceVersion,
        deviceDetail = this.deviceDetail,
        deviceToken = this.deviceToken,
        locationToken = this.locationToken,
        aqnsToken = this.aqnsToken,
        apiToken = this.apiToken,
        agreeService = this.agreeService,
        agreePrivacy = this.agreePrivacy,
        agreeFinance = this.agreeFinance,
        agreeMarketing = this.agreeMarketing,
        pushService = this.pushService,
        pushMarketing = this.pushMarketing,
        pushItems = parsePushItems(this.pushItems),
        memo = this.memo,
    )

    private fun parsePushItems(pushItems: String?): List<String> {
        if (pushItems.isNullOrBlank()) return emptyList()

        return try {
            objectMapper.readValue(pushItems, object : TypeReference<List<String>>() {})
                .filter { it.isNotBlank() } // 빈 항목 제거
        } catch (e: Exception) {
            emptyList()
        }
    }



}
