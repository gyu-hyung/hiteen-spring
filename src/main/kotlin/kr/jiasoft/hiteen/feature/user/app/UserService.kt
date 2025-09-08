package kr.jiasoft.hiteen.feature.user.app

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.toResponse
import kr.jiasoft.hiteen.feature.user.domain.toResponseWithSchool
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
    private val schoolRepository: SchoolRepository,
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> = mono {
        val user = userRepository.findByUsername(username)
        user?.toUserDetails() ?: throw UsernameNotFoundException("User not found: $username")
    }

    suspend fun nicknameDuplicationCheck(nickname: String): Boolean {
        val user = userRepository.findAllByNickname(nickname).firstOrNull()
        return user != null
    }


    suspend fun register(param: UserRegisterForm, file: FilePart?): UserResponse {
        val exists = nicknameDuplicationCheck(param.nickname!!)
        if (exists) {
            throw BusinessValidationException(mapOf("nickname" to "이미 사용 중인 닉네임입니다."))
        }

        val toEntity = param.toEntity(encoder.encode(param.password))
        val saved = userRepository.save(toEntity)

        val updated: UserEntity = if (file != null) {
            val asset = assetService.uploadImage(
                file = file,
                originFileName = null,
                currentUserId = saved.id!!
            )
            userRepository.save(saved.copy(assetUid = asset.uid))
        } else {
            saved
        }

        val school = updated.schoolId?.let { id -> schoolRepository.findById(id) }
        return updated.toResponseWithSchool(school)
    }


    suspend fun findMe(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            ?: throw UsernameNotFoundException("User not found: $userId")

        val school = user.schoolId?.let { id -> schoolRepository.findById(id) }
        return user.toResponseWithSchool(school)
    }


    // TODO 회원 정보 변경 시 로그아웃(토큰 무효화) 기준 정리 필요: 비밀번호/권한/중요 개인정보 변경 시 재로그인 유도 등
    suspend fun updateUser(current: UserEntity, param: UserUpdateForm, part: FilePart?): UserResponse {

        val existing = userRepository.findById(current.id!!)
            ?: throw UsernameNotFoundException("User not found: ${current.username}")

        // 1) 파일 업로드 처리
        var newAssetUid: UUID? = param.assetUid ?: existing.assetUid
        var oldAssetUidToDelete: UUID? = null

        if (part != null) {
            val uploaded = assetService.uploadImage(
                file = part,
                originFileName = null,
                currentUserId = current.id
            )
            oldAssetUidToDelete = existing.assetUid
            newAssetUid = uploaded.uid
        }

        // 2) 변경값 준비 (null이면 기존값 유지)
        val newUsername    = param.username?.trim()?.takeIf { it.isNotEmpty() } ?: existing.username
        val newEmail       = param.email?.trim()?.takeIf { it.isNotEmpty() } ?: existing.email
        val newNickname    = param.nickname ?: existing.nickname
        val newPassword    = param.password?.let { encoder.encode(it) } ?: existing.password
        val newAddress     = param.address ?: existing.address
        val newDetailAddr  = param.detailAddress ?: existing.detailAddress
        val newMood        = param.mood ?: existing.mood
        val newTier        = param.tier ?: existing.tier
        val newSchoolId    = param.schoolId ?: existing.schoolId
        val newGrade       = param.grade ?: existing.grade
        val newGender      = param.gender ?: existing.gender
        val newBirthday    = param.birthday ?: existing.birthday

        // 중복 검사
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

        // 3) 엔티티 복사
        val updated = existing.copy(
            username      = newUsername,
            email         = newEmail,
            nickname      = newNickname,
            password      = newPassword,
            address       = newAddress,
            detailAddress = newDetailAddr,
            mood          = newMood,
            tier          = newTier,
            assetUid      = newAssetUid,
            schoolId      = newSchoolId,
            grade         = newGrade,
            gender        = newGender,
            birthday      = newBirthday,
            updatedId     = current.id,
            updatedAt     = OffsetDateTime.now(),
        )

        val saved = userRepository.save(updated)

        // 4) 기존 프로필 이미지 소프트 삭제
        if (oldAssetUidToDelete != null) {
            try { assetService.softDelete(oldAssetUidToDelete, current.id) } catch (_: Throwable) {}
        }

        // 5) schoolId 있으면 조회해서 DTO 변환
        val school = saved.schoolId?.let { id -> schoolRepository.findById(id) }
        return saved.toResponseWithSchool(school)
    }




}