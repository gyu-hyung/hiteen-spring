package kr.jiasoft.hiteen.feature.user.app

import kr.jiasoft.hiteen.feature.user.domain.UserDetailEntity
import kr.jiasoft.hiteen.feature.user.dto.UserDetailRequest
import kr.jiasoft.hiteen.feature.user.dto.UserDetailResponse
import kr.jiasoft.hiteen.feature.user.infra.UserDetailRepository
import org.springframework.stereotype.Service

@Service
class UserDetailService(
    private val userDetails: UserDetailRepository
) {

    suspend fun getUserDetail(userId: Long): UserDetailResponse? {
        val entity = userDetails.findByUserId(userId) ?: return null
        return entity.toResponse()
    }

    suspend fun upsertUserDetail(userId: Long, req: UserDetailRequest): UserDetailResponse {
        val existing = userDetails.findByUserId(userId)
        val saved = if (existing != null) {
            existing.copy(
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
                memo = req.memo ?: existing.memo,
            )
        } else {
            UserDetailEntity(
                userId = userId,
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
                memo = req.memo,
            )
        }
        return userDetails.save(saved).toResponse()
    }

    suspend fun deleteUserDetail(userId: Long) {
        userDetails.deleteByUserId(userId)
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
        memo = this.memo,
    )
}
