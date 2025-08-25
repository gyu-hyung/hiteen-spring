package kr.jiasoft.hiteen.feature.user.app

import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.toResponse
import kr.jiasoft.hiteen.feature.user.domain.toUserDetails
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.OffsetDateTime

@Service
class UserService (
    private val userRepository: UserRepository,
    private val encoder: PasswordEncoder
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> = mono {
        val user = userRepository.findByUsername(username)
        user?.toUserDetails() ?: throw UsernameNotFoundException("User not found: $username")
    }

    suspend fun register(param: UserRegisterForm): UserResponse {
        val entity = param.toEntity(encoder.encode(param.password))
        return userRepository.save(entity).toResponse()
    }

    // TODO 회원 정보 변경 시 로그아웃(토큰 무효화) 기준 정리 필요: 비밀번호/권한/중요 개인정보 변경 시 재로그인 유도 등
    suspend fun updateUser(current: UserEntity, param: UserUpdateForm): UserResponse {
        val existing = userRepository.findById(current.id!!)
            ?: throw UsernameNotFoundException("User not found: ${current.username}")

        // 변경값 준비 (null이면 기존값 유지)
        val newUsername    = param.username?.trim()?.takeIf { it.isNotEmpty() } ?: existing.username
        val newEmail       = param.email?.trim()?.takeIf { it.isNotEmpty() } ?: existing.email
        val newNickname    = param.nickname ?: existing.nickname
        val newPassword    = param.password?.let { encoder.encode(it) } ?: existing.password
        val newAddress     = param.address ?: existing.address
        val newDetailAddr  = param.detailAddress ?: existing.detailAddress
        val newTelno       = param.telno ?: existing.telno
        val newMood        = param.mood ?: existing.mood
        val newTier        = param.tier ?: existing.tier
        val newAssetUid    = param.assetUid ?: existing.assetUid

        val updated = existing.copy(
            /* username */       username      = newUsername,
            /* email */          email         = newEmail,
            /* nickname */       nickname      = newNickname,
            /* password */       password      = newPassword,
            /* address */        address       = newAddress,
            /* detail_address */ detailAddress = newDetailAddr,
            /* telno */          telno         = newTelno,
            /* mood */           mood          = newMood,
            /* tier */           tier          = newTier,
            /* asset_uid */      assetUid      = newAssetUid,
            /* updated_id */     updatedId     = current.id,
            /* updated_at */     updatedAt     = OffsetDateTime.now(),
        )

        return userRepository.save(updated).toResponse()
    }



}