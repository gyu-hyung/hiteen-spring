package kr.jiasoft.hiteen.feature.invite.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.invite.domain.InviteEntity
import kr.jiasoft.hiteen.feature.invite.infra.InviteRepository
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.random.Random

@Service
class InviteService(
    private val inviteRepository: InviteRepository,
    private val userRepository: UserRepository,
    private val expService: ExpService,
    private val pointService: PointService,
) {


    /** 내 초대코드로 등록한 친구 목록 */
    suspend fun findMyReferralList(userId: Long): List<Pair<Long, OffsetDateTime>> {
        return inviteRepository.findAllByUserId(userId)
            .map { it.joinId to it.joinDate }
            .toList()
    }


    /** 초대 완료 시 경험치 부여 TODO 보안 */
    suspend fun giveInviteExp(userId: Long, targetUid: UUID) {
        userRepository.findByUid(targetUid.toString())?.let { user ->
            expService.grantExp(userId, "FRIEND_INVITE", user.id)
        }
    }

    /**
     * 초대코드로 회원가입한 경우 처리
     * @return 초대자 userId (성공 시), 실패 시 null
     */
    suspend fun handleInviteJoin(newUser: UserEntity, code: String): Long? {
        if (newUser.phone.isBlank() || code.isBlank()) {
            return null
        }

        // 이미 초대코드 이벤트에 참여한 휴대폰인지 체크 (중복 지급 방지)
        val invitedCount = inviteRepository.countByPhone(newUser.phone)
        val invitePoint = if (invitedCount < 1) {
            PointPolicy.FRIEND_INVITE.amount
        }else {
            0
        }

        // 초대한 회원 찾기
        val inviter = userRepository.findByInviteCode(code) ?: return null

        // 초대 내역 저장
        val invite = InviteEntity(
            type = "Register",
            userId = inviter.id, // 초대한 회원
            phone = newUser.phone,
            code = code,
            status = 1,
            joinId = newUser.id, // 초대받은 회원
            joinPoint = invitePoint,
            joinDate = OffsetDateTime.now()
        )
        inviteRepository.save(invite)

        // 초대한 회원의 가입자 수 +1
        val updatedInviter = inviter.copy(inviteJoins = inviter.inviteJoins + 1)
        userRepository.save(updatedInviter)

        // point +1000
        pointService.applyPolicy(inviter.id, PointPolicy.FRIEND_INVITE, newUser.id)
        // exp +30
        expService.grantExp(inviter.id, "FRIEND_INVITE", newUser.id)

        return inviter.id
    }


    /**
     * 초대코드 생성
     * - 접두사 "T"
     * - 대문자 + 숫자 9자리
     * - 최대 10회 시도
     */
    private suspend fun generalInviteCode(): String? {
        var attempts = 0
        while (attempts < 10) {
            val code = "T" + randomUpperNumeric(9)
            val exists = userRepository.findByInviteCode(code) != null
            if (!exists) return code
            attempts++
        }
        return null
    }

    /**
     * 초대코드 업데이트
     * - force=false이고 이미 코드 있으면 그대로 둠
     * - 없다면 새 코드 생성 후 저장
     */
    suspend fun updateInviteCode(user: UserEntity, force: Boolean = false, isSave: Boolean = true): Boolean {
        if (!force && !user.inviteCode.isNullOrBlank()) {
            return true
        }

        return try {
            val code = generalInviteCode()
            if (code != null) {
                val updatedUser = user.copy(inviteCode = code)
                if (isSave) {
                    userRepository.save(updatedUser)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 초대코드 등록
     * - 이미 있으면 유지
     * - 없으면 updateInviteCode() 실행 (최대 10회 시도)
     */
    suspend fun registerInviteCode(user: UserEntity, force: Boolean = false, isSave: Boolean = true): Boolean {
        if (!force && !user.inviteCode.isNullOrBlank()) {
            return true
        }

        var attempts = 0
        while (attempts < 10) {
            val success = updateInviteCode(user, force, isSave)
            if (success) {

                break
            }
            attempts++
        }

        return true
    }

    /**
     * 대문자 + 숫자 랜덤 문자열 생성
     */
    private fun randomUpperNumeric(length: Int): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { chars.random(Random) }
            .joinToString("")
    }
}
