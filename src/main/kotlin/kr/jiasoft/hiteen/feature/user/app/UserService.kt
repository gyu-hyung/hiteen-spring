package kr.jiasoft.hiteen.feature.user.app

import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.toResponse
import kr.jiasoft.hiteen.feature.user.domain.toUserDetails
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.util.UUID

@Service
class UserService (
    private val assetService: AssetService,
    private val userRepository: UserRepository,
    private val encoder: PasswordEncoder,
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> = mono {
        val user = userRepository.findByUsername(username)
        user?.toUserDetails() ?: throw UsernameNotFoundException("User not found: $username")
    }

    suspend fun nicknameDuplicationCheck(nickname: String): Boolean {
        val user = userRepository.findByNickname(nickname)
        return user != null
    }

//    @Transactional
    suspend fun register(param: UserRegisterForm, file: FilePart?): UserResponse {
        val toEntity = param.toEntity(encoder.encode(param.password))
        val saved = userRepository.save(toEntity)

        val updated: UserEntity = if (file != null) {
            val asset = assetService.uploadImage(
                file = file,
                originFileName = null,
                currentUserId = saved.id!!          // assets.created_id 로 사용
            )
            val withAsset = saved.copy(assetUid = asset.uid) // 컬럼명이 다르면 필드명 맞춰줘요 (@Column("asset_uid"))
            userRepository.save(withAsset)
        } else {
            saved
        }
        return updated.toResponse()
    }

    // TODO 회원 정보 변경 시 로그아웃(토큰 무효화) 기준 정리 필요: 비밀번호/권한/중요 개인정보 변경 시 재로그인 유도 등
    suspend fun updateUser(current: UserEntity, param: UserUpdateForm, part: FilePart?): UserResponse {

        val existing = userRepository.findById(current.id!!)
            ?: throw UsernameNotFoundException("User not found: ${current.username}")

        // 1) 파일이 있으면: 이미지 유효성 검사 + 업로드 → 새 uid 획득
        var newAssetUid: UUID? = param.assetUid ?: existing.assetUid
        var oldAssetUidToDelete: UUID? = null

        if (part != null) {
            val uploaded = assetService.uploadImage(
                file = part,
                originFileName = null,
                currentUserId = current.id
            )
            // 기존과 다르면 교체 + 기존은 이후 soft delete
            if (existing.assetUid != null && existing.assetUid != uploaded.uid) {
                oldAssetUidToDelete = existing.assetUid
            }
            newAssetUid = uploaded.uid
        }

        // 2) 변경값 준비 (null이면 기존값 유지)
        val newUsername    = param.username?.trim()?.takeIf { it.isNotEmpty() } ?: existing.username
        val newEmail       = param.email?.trim()?.takeIf { it.isNotEmpty() } ?: existing.email
        val newNickname    = param.nickname ?: existing.nickname
        val newPassword    = param.password?.let { encoder.encode(it) } ?: existing.password
        val newAddress     = param.address ?: existing.address
        val newDetailAddr  = param.detailAddress ?: existing.detailAddress
        val newTelno       = param.telno ?: existing.telno
        val newMood        = param.mood ?: existing.mood
        val newTier        = param.tier ?: existing.tier

        // 변경된 경우에만 중복검사 (대소문자 무시)
        if (newEmail != null && !newEmail.equals(existing.email, ignoreCase = true)) {
            if (userRepository.existsByEmailIgnoreCaseAndActiveAndIdNot(newEmail, existing.id!!)) {
                throw BusinessValidationException(mapOf("email" to "이미 사용 중인 이메일입니다."))
            }
        }
        if (!newUsername.equals(existing.username, ignoreCase = true)) {
            if (userRepository.existsByUsernameIgnoreCaseAndActiveAndIdNot(newUsername, existing.id!!)) {
                throw BusinessValidationException(mapOf("username" to "이미 사용 중인 사용자명입니다."))
            }
        }

        val updated = existing.copy(
            username      = newUsername,
            email         = newEmail,
            nickname      = newNickname,
            password      = newPassword,
            address       = newAddress,
            detailAddress = newDetailAddr,
            telno         = newTelno,
            mood          = newMood,
            tier          = newTier,
            assetUid      = newAssetUid,
            updatedId     = current.id,
            updatedAt     = OffsetDateTime.now(),
        )

        val saved = userRepository.save(updated)

        // 3) 기존 프로필 이미지가 있었다면 소프트 삭제 (실패해도 사용자 업데이트는 유지)
        if (oldAssetUidToDelete != null) {
            try { assetService.softDelete(oldAssetUidToDelete, current.id) } catch (_: Throwable) {}
        }

        return saved.toResponse()
    }



}