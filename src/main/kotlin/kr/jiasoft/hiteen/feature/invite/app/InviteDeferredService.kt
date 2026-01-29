package kr.jiasoft.hiteen.feature.invite.app

import kr.jiasoft.hiteen.feature.invite.domain.InviteLinkTokenEntity
import kr.jiasoft.hiteen.feature.invite.infra.InviteLinkTokenRepository
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.Base64

@Service
class InviteDeferredService(
    private val inviteLinkTokenRepository: InviteLinkTokenRepository,
) {
    private val random = SecureRandom()

    /**
     * Android Install Referrer에 담기 위한 짧은 토큰을 발급합니다.
     * - 만료: 기본 7일
     * - 1회 사용(클라이언트가 resolve 시 used 처리 권장)
     */
    suspend fun issueToken(code: String, ttlDays: Long = 7): String {
        val token = generateToken()
        inviteLinkTokenRepository.save(
            InviteLinkTokenEntity(
                token = token,
                code = code,
                expiresAt = OffsetDateTime.now().plusDays(ttlDays),
            )
        )
        return token
    }

    /**
     * 토큰을 초대코드로 해석합니다. (만료/사용 처리)
     * @param userId 선택: 사용자를 알 수 있으면 기록
     */
    suspend fun resolveToken(token: String, userId: Long? = null): String? {
        val row = inviteLinkTokenRepository.findByToken(token) ?: return null

        if (row.usedAt != null) return null
        if (OffsetDateTime.now().isAfter(row.expiresAt)) return null

        inviteLinkTokenRepository.save(
            row.copy(
                usedAt = OffsetDateTime.now(),
                usedBy = userId,
            )
        )
        return row.code
    }

    private fun generateToken(): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        // URL-safe base64, padding 제거
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

