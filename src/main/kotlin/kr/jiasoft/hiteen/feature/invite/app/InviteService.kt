package kr.jiasoft.hiteen.feature.invite.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.invite.domain.InviteEntity
import kr.jiasoft.hiteen.feature.invite.infra.InviteRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import kotlin.random.Random

@Service
class InviteService(
    private val inviteRepository: InviteRepository,
    private val userRepository: UserRepository,
) {


    /** 내 초대코드로 등록한 친구 목록 */
    suspend fun findMyReferralList (userId: Long) : List<Long> {
        return inviteRepository.findAllByUserId(userId).map { it.id }.toList()
    }

    /**
     * 초대코드로 회원가입한 경우 처리
     */
    suspend fun handleInviteJoin(newUser: UserEntity, code: String): Boolean {
        if (newUser.phone.isBlank() || code.isBlank()) {
            return false
        }

        // 이미 초대코드 이벤트에 참여한 휴대폰인지 체크 (중복 지급 방지)
        val invitedCount = inviteRepository.countByPhone(newUser.phone)
        var invitePoint = 0
        if (invitedCount < 1) {
            invitePoint = getInviteJoinPoint() // 설정값 또는 상수 (예: 100)
        }

        // 초대한 회원 찾기
        val inviter = userRepository.findByInviteCode(code) ?: return false

        // 초대 내역 저장
        val invite = InviteEntity(
            type = "Register",
            userId = inviter.id,//초대한 회원
            phone = newUser.phone,
            code = code,
            status = 1,
            joinId = newUser.id,//초대받은 회원
            joinPoint = invitePoint,
            joinDate = OffsetDateTime.now()
        )
        inviteRepository.save(invite)

        // 초대한 회원의 가입자 수 +1
        val updatedInviter = inviter.copy(inviteJoins = inviter.inviteJoins + 1)
        userRepository.save(updatedInviter)

        // TODO: 포인트 지급 처리 (PointService 등과 연동)
//        if (invitePoint > 0) {
            // pointService.give(inviter, invitePoint, "Invite", invite.id, "[친구초대] ${newUser.nickname}님이 초대코드로 가입했어요!")
//        }

        // TODO: PushService 연동 시 알림 발송
        // pushService.sendUser(newUser, inviter, message)

        return true
    }

    //TODO 포인트 상수화
    private fun getInviteJoinPoint(): Int {
        return 100
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
            if (!exists) {
                return code
            }
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
