package kr.jiasoft.hiteen.feature.user.app

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.mono
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.invite.app.InviteService
import kr.jiasoft.hiteen.feature.level.domain.TierCode
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.relationship.app.FollowService
import kr.jiasoft.hiteen.feature.relationship.app.FriendService
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import kr.jiasoft.hiteen.feature.user.dto.CustomUserDetails
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import kr.jiasoft.hiteen.feature.user.infra.UserPhotosRepository
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
    private val encoder: PasswordEncoder,
    private val assetService: AssetService,
    private val followService: FollowService,
    private val friendService: FriendService,

    private val userRepository: UserRepository,
    private val userPhotosRepository: UserPhotosRepository,
    private val schoolRepository: SchoolRepository,
    private val interestUserRepository: InterestUserRepository,
    private val boardRepository: BoardRepository,
    private val inviteService: InviteService,
    private val tierRepository: TierRepository,
    private val pointService: PointService,
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> = mono {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        CustomUserDetails.from(user)
    }

    suspend fun findByUid(uid: String): UserEntity? {
        return userRepository.findByUid(uid)
    }

    suspend fun nicknameDuplicationCheck(nickname: String): Boolean {
        val user = userRepository.findAllByNickname(nickname).firstOrNull()
        return user != null
    }

    suspend fun phoneDuplicationCheck(phone: String): Boolean {
        val user = userRepository.findAllByPhone(phone).firstOrNull()
        return user != null
    }


    suspend fun register(param: UserRegisterForm, file: FilePart?): UserResponse {
        val inviteCode = param.inviteCode
        param.inviteCode = null

        val nicknameExists = nicknameDuplicationCheck(param.nickname)
        if (nicknameExists) {
            throw BusinessValidationException(mapOf("nickname" to "이미 사용 중인 닉네임입니다."))
        }

        val phoneExists = phoneDuplicationCheck(param.phone)
        if (phoneExists) {
            throw BusinessValidationException(mapOf("phone" to "이미 사용 중인 휴대폰 번호입니다."))
        }

        val school = param.schoolId?.let { id -> schoolRepository.findById(id) }
        val tier = tierRepository.findByTierCode(TierCode.BRONZE_STAR)
        val toEntity = param.toEntity(encoder.encode(param.password), tier.id)
        val saved = userRepository.save(toEntity)

        val updated: UserEntity = if (file != null) {
            val asset = assetService.uploadImage(
                file = file,
                originFileName = null,
                currentUserId = saved.id
            )
            userRepository.save(saved.copy(assetUid = asset.uid))
        } else {
            saved
        }

        // 초대코드 생성
        inviteService.registerInviteCode(updated)

        //초대코드를 통해 회원가입 할 경우
        if(!inviteCode.isNullOrBlank()) {
            val success = inviteService.handleInviteJoin(updated, inviteCode)
            if (!success) {
                throw BusinessValidationException(
                    mapOf("inviteCode" to "유효하지 않은 초대코드입니다.")
                )
            }
        }

        //포인트 지급
        pointService.applyPolicy(updated.id, PointPolicy.SIGNUP)

        return UserResponse.from(updated, school)
    }


    suspend fun findUserResponse(targetUid: UUID, currentUserId: Long? = null): UserResponse {
        val targetUser = userRepository.findByUid(targetUid.toString())
            ?: throw UsernameNotFoundException("User not found: $targetUid")

        val school = targetUser.schoolId?.let { id -> schoolRepository.findById(id) }
        val tier = tierRepository.findById(targetUser.tierId)
        val interests = interestUserRepository.getInterestResponseById(null, targetUser.id).toList()
        val relationshipCounts = followService.getRelationshipCounts(targetUser.id).copy(
            postCount = boardRepository.countByCreatedId(targetUser.id)
        )
        val photos = getPhotosById(targetUser.id)

        val isFollowed = currentUserId?.let { followService.isFollowing(it, targetUser.id) } ?: false
        val isFriend = currentUserId?.let { friendService.isFriend(it, targetUser.id) } ?: false

        return UserResponse.from(targetUser, school, tier, interests, relationshipCounts, photos, isFollowed, isFriend)
    }


    suspend fun findUserSummary(userId: Long): UserSummary {
        return userRepository.findSummaryInfoById(userId)
    }


    suspend fun findUserSummaryList(userIds: List<Long>)
        = userRepository.findSummaryByIds(userIds)



    // TODO 회원 정보 변경 시 로그아웃(토큰 무효화) 기준 정리 필요: 비밀번호/권한/중요 개인정보 변경 시 재로그인 유도 등
    suspend fun updateUser(current: UserEntity, param: UserUpdateForm, part: FilePart?): UserResponse {

        val existing = userRepository.findById(current.id)
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
        val newMoodEmoji   = param.moodEmoji ?: existing.moodEmoji
        val newSchoolId    = param.schoolId ?: existing.schoolId
        val newGrade       = param.grade ?: existing.grade
        val newGender      = param.gender ?: existing.gender
        val newBirthday    = param.birthday ?: existing.birthday
        val newProfileDecorationCode = param.profileDecorationCode ?: existing.profileDecorationCode

        // 중복 검사
        if (newEmail != null && !newEmail.equals(existing.email, ignoreCase = true)) {
            if (userRepository.existsByEmailIgnoreCaseAndActiveAndIdNot(newEmail, existing.id)) {
                throw BusinessValidationException(mapOf("email" to "이미 사용 중인 이메일입니다."))
            }
        }
        if (!newUsername.equals(existing.username, ignoreCase = true)) {
            if (userRepository.existsByUsernameIgnoreCaseAndActiveAndIdNot(newUsername, existing.id)) {
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
            moodEmoji     = newMoodEmoji,
            assetUid      = newAssetUid,
            schoolId      = newSchoolId,
            grade         = newGrade,
            gender        = newGender,
            birthday      = newBirthday,
            profileDecorationCode = newProfileDecorationCode,
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
        val tier = tierRepository.findById(current.tierId)
        val interests = interestUserRepository.getInterestResponseById(null, saved.id).toList()
        val relationshipCounts = followService.getRelationshipCounts(saved.id)
        val photos = getPhotosById(saved.id)

        return UserResponse.from(saved, school, tier, interests, relationshipCounts, photos)
    }


    suspend fun registerPhotos(user: UserEntity, files: List<FilePart>?) {
        if (files.isNullOrEmpty()) {
            throw BusinessValidationException(mapOf("file" to "이미지가 필요합니다."))
        }

        files.forEach { file ->
            // 1) 에셋 업로드
            val asset = assetService.uploadImage(
                file = file,
                originFileName = null,
                currentUserId = user.id
            )

            // 2) user_photos row 생성
            val photoEntity = UserPhotosEntity(
                userId = user.id,
                uid = asset.uid
            )

            userPhotosRepository.save(photoEntity)
        }
    }


    suspend fun deletePhoto(user: UserEntity, photoId: Long) {
        val exist = userPhotosRepository.findByIdAndUserId(photoId, user.id)
            ?: throw BusinessValidationException(mapOf("photo" to "존재하지 않거나 본인 소유가 아닌 사진입니다."))

        // asset 파일도 소프트 삭제
        try {
            assetService.softDelete(exist.uid, user.id)
        } catch (_: Throwable) {}

        // user_photos row 삭제
        userPhotosRepository.deleteById(exist.id)
    }


    suspend fun getPhotosById(userId: Long): List<UserPhotosEntity> {
        val flow = userPhotosRepository.findByUserId(userId)?.toList()
        return flow?.toList() ?: emptyList()
    }


    suspend fun getPhotos(userUid: String): List<UserPhotosEntity> {
        val userEntity = userRepository.findByUid(userUid)
            ?: throw BusinessValidationException(mapOf("user" to "존재하지 않는 사용자입니다."))

        return userPhotosRepository.findByUserId(userEntity.id)?.toList() ?: emptyList()
    }

    suspend fun myReferralList(userId: Long): List<UserSummary> {
        val ids = inviteService.findMyReferralList(userId).ifEmpty { return emptyList() }
        return findUserSummaryList(ids)
    }


}
