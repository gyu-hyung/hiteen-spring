package kr.jiasoft.hiteen.feature.user.app

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.auth.dto.JwtResponse
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
import kr.jiasoft.hiteen.feature.interest.app.InterestUserService
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.invite.app.InviteService
import kr.jiasoft.hiteen.feature.level.domain.TierCode
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.relationship.domain.FollowStatus
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipCounts
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import kr.jiasoft.hiteen.feature.user.dto.ReferralSummary
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserResponseWithTokens
import kr.jiasoft.hiteen.feature.user.dto.UserSummary
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import kr.jiasoft.hiteen.feature.user.infra.UserPhotosRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
class UserService (
    private val encoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val assetService: AssetService,
    private val followRepository: FollowRepository,
    private val friendRepository: FriendRepository,

    private val userRepository: UserRepository,
    private val userPhotosRepository: UserPhotosRepository,
    private val schoolRepository: SchoolRepository,
    private val interestUserRepository: InterestUserRepository,
    private val boardRepository: BoardRepository,
    private val inviteService: InviteService,
    private val tierRepository: TierRepository,
    private val pointService: PointService,
    private val interestUserService: InterestUserService,
) {


    @Value("\${app.join.rejoin-days:30}")
    private val rejoinDays: Int = 30

    @Value("\${app.join.dev-allow-always:false}")  // 개발중엔 true로 두면 언제든 새로 가입
    private val devAllowAlways: Boolean = false


    suspend fun findByUid(uid: String): UserEntity? {
        return userRepository.findByUid(uid)
    }

    suspend fun nicknameDuplicationCheck(nickname: String): Boolean {
        val user = userRepository.findAllByNickname(nickname).firstOrNull()
        return user != null
    }

//    suspend fun phoneDuplicationCheck(phone: String): Boolean {
//        val user = userRepository.findAllByPhone(phone).firstOrNull()
//        return user != null
//    }

    /** 삭제되지 않은 휴대폰 번호가 있는지 확인 */
    suspend fun phoneDuplicationCheckActiveOnly(phone: String): Boolean {
        return userRepository.findActiveByPhone(phone) != null
    }


    private suspend fun toUserResponse(targetUser: UserEntity, currentUserId: Long? = null): UserResponse {
        val school = targetUser.schoolId?.let { id -> schoolRepository.findById(id) }
        val tier = tierRepository.findById(targetUser.tierId)
        val interests = interestUserRepository.getInterestResponseById(null, targetUser.id).toList()
        val relationshipCounts = RelationshipCounts(
            postCount = boardRepository.countByCreatedId(targetUser.id),
            followerCount = followRepository.countByFollowIdAndStatus(targetUser.id, FollowStatus.ACCEPTED.name),
            followingCount = followRepository.countByUserIdAndStatus(targetUser.id, FollowStatus.ACCEPTED.name),
        )
        val photos = getPhotosById(targetUser.id)

        val isFollowed = currentUserId?.let { followRepository.existsFollow(it, targetUser.id) > 0 }  ?: false
        val isFriend = currentUserId?.let { friendRepository.existsFriend(it, targetUser.id) > 0 } ?: false

        return UserResponse.from(targetUser, school, tier, interests, relationshipCounts, photos, isFollowed, isFriend)
    }

    suspend fun findUserResponse(username: String): UserResponse {
        val targetUser = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return toUserResponse(targetUser)
    }


    //    @Cacheable(cacheNames = ["userResponse"], key = "#targetId")
    suspend fun findUserResponse(targetId: Long, currentUserId: Long? = null): UserResponse {
        println("🧠  findUserResponse(targetId  캐시 미적용 - 실제 DB 조회 발생! Thread = ${Thread.currentThread().name}")

        val targetUser = userRepository.findById(targetId)
            ?: throw UsernameNotFoundException("User not found: $targetId")

        return toUserResponse(targetUser, currentUserId)
    }


    @Cacheable(cacheNames = ["userResponse"], key = "#targetUid")
    suspend fun findUserResponse(targetUid: UUID, currentUserId: Long? = null): UserResponse {
        println("🧠 findUserResponse(targetUid 캐시 미적용 - 실제 DB 조회 발생! Thread = ${Thread.currentThread().name}")
        val targetUser = userRepository.findByUid(targetUid.toString())
            ?: throw UsernameNotFoundException("User not found: $targetUid")

        return toUserResponse(targetUser, currentUserId)
    }

    @Cacheable(cacheNames = ["userSummary"], key = "#userId")
    suspend fun findUserSummary(userId: Long): UserSummary {
        println("🧠 findUserSummary(userId 캐시 미적용 - 실제 DB 조회 발생! Thread = ${Thread.currentThread().name}")
        return userRepository.findSummaryInfoById(userId)
    }

    @Cacheable(cacheNames = ["userEntity"], key = "#username")
    suspend fun findByUsernamee(username: String): UserEntity {
        println("🧠 findByUsernamee(username 캐시 미적용 - 실제 DB 조회 발생! Thread = ${Thread.currentThread().name}")
        return userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")
    }


    suspend fun register(param: UserRegisterForm, file: FilePart?): UserResponseWithTokens {
        val now = OffsetDateTime.now()

        // username == phone 정책
        val phone = param.phone.trim()
        param.username = phone

        // 닉네임 중복(활성만)
        if (userRepository.existsByNicknameActive(param.nickname)) {
            throw BusinessValidationException(mapOf("nickname" to "이미 사용 중인 닉네임입니다."))
        }
        // 휴대폰(=username) 중복(활성만)
        if (phoneDuplicationCheckActiveOnly(phone)) {
            throw BusinessValidationException(mapOf("phone" to "이미 사용 중인 휴대폰 번호입니다."))
        }

        // 최근 탈퇴 사용자 조회
        val latestDeleted = userRepository.findLatestDeletedByPhone(phone)

        // 개발 모드가 아니고, 최근 탈퇴가 존재하며, 30일 경과 여부로 복구/신규 분기
        val canAlways = devAllowAlways
        val shouldRestore =
            latestDeleted != null &&
                    (canAlways || now.isBefore(latestDeleted.deletedAt!!.plusDays(rejoinDays.toLong())))

        // 초대코드 분리 (신규만 처리)
        val inviteCode = param.inviteCode
        param.inviteCode = null

        return if (shouldRestore) {
            // =========================
            // A) 계정 복구(삭제 해제)
            // =========================
            val existing = latestDeleted!!

            // 비밀번호/닉네임 등 가입 폼에서 온 값으로 업데이트할지 정책 결정:
            // - 보통 복구는 기록 보존을 위해 최소 변경만 권장.
            // - 여기선 "비밀번호는 새로 설정 가능"하게 반영 예시.
            val updated = existing.copy(
                // username/phone은 동일 유지 (중복 Unique 일관성)
                // 사용자가 새 비번을 입력했다면 갱신
                password = encoder.encode(param.password),
                nickname = param.nickname.ifBlank { existing.nickname },
                email = param.email ?: existing.email,
                // 프로필은 파일 있으면 교체
                // assetUid는 아래에서 파일 업로드 후 set
                updatedAt = now,
                updatedId = existing.id,
                deletedAt = null,
                deletedId = null
            )

            val saved = userRepository.save(updated)

            // 프로필 이미지 갱신 (선택)
            val finalSaved = if (file != null) {
                val asset = assetService.uploadImage(file, saved.id, AssetCategory.PROFILE)
                userRepository.save(saved.copy(assetUid = asset.uid))
            } else saved

            // JWT
            val (access, refresh) = jwtProvider.generateTokens(finalSaved.username)

            // 복구 시에는 관심사/초대코드/포인트 등은 **기존 데이터 유지**가 일반적
            val responseUser = findUserResponse(finalSaved.id)

            UserResponseWithTokens(
                tokens = JwtResponse(access.value, refresh.value),
                userResponse = responseUser
            )
        } else {
            // =========================
            // B) 신규 생성 (30일 이후)
            // =========================
            val school = param.schoolId?.let { id -> schoolRepository.findById(id) }
            val tier = tierRepository.findByTierCode(TierCode.BRONZE_STAR)

            val toEntity = param.toEntity(encoder.encode(param.password), tier.id)
            val saved = userRepository.save(toEntity)

            val updated: UserEntity = if (file != null) {
                val asset = assetService.uploadImage(file, saved.id, AssetCategory.PROFILE)
                userRepository.save(saved.copy(assetUid = asset.uid))
            } else saved

            // 기본 관심사
            interestUserService.initDefaultInterests(updated)
            // 초대코드 생성
            inviteService.registerInviteCode(updated)
            // 초대코드로 가입 처리
            if (!inviteCode.isNullOrBlank()) {
                val ok = inviteService.handleInviteJoin(updated, inviteCode)
                if (!ok) throw BusinessValidationException(mapOf("inviteCode" to "유효하지 않은 초대코드입니다."))
            }
            // JWT
            val (access, refresh) = jwtProvider.generateTokens(updated.username)
            // 포인트 지급
            pointService.applyPolicy(updated.id, PointPolicy.SIGNUP)

            val responseUser = userRepository.findById(updated.id)?.let {
                UserResponse.from(it, school, tier)
            } ?: UserResponse.empty()

            UserResponseWithTokens(
                tokens = JwtResponse(access.value, refresh.value),
                userResponse = responseUser.copy(inviteCode = updated.inviteCode)
            )
        }
    }



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
                currentUserId = current.id,
                AssetCategory.PROFILE
            )
            oldAssetUidToDelete = existing.assetUid
            newAssetUid = uploaded.uid
        }

        // 2) 변경값 준비 (null이면 기존값 유지)
//        val newUsername    = param.username?.trim()?.takeIf { it.isNotEmpty() } ?: existing.username
        val newEmail       = param.email?.trim()?.takeIf { it.isNotEmpty() } ?: existing.email
        val newNickname    = param.nickname ?: existing.nickname
        val newPassword    = param.password?.let { encoder.encode(it) } ?: existing.password
        val newAddress     = param.address ?: existing.address
        val newDetailAddr  = param.detailAddress ?: existing.detailAddress
//        val newPhone       = param.phone ?: existing.phone
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
//        if (newPhone != null && !newPhone.equals(existing.phone, ignoreCase = true)) {
//            if (userRepository.existsByPhoneAndActiveAndIdNot(newPhone, existing.id)) {
//                throw BusinessValidationException(mapOf("phone" to "이미 사용 중인 전화번호입니다."))
//            }
//        }
//        if (!newUsername.equals(existing.username, ignoreCase = true)) {
//            if (userRepository.existsByUsernameIgnoreCaseAndActiveAndIdNot(newUsername, existing.id)) {
//                throw BusinessValidationException(mapOf("username" to "이미 사용 중인 사용자명입니다."))
//            }
//        }

        // 3) 엔티티 복사
        val updated = existing.copy(
//            username      = newUsername,
            email         = newEmail,
            nickname      = newNickname,
            password      = newPassword,
            address       = newAddress,
            detailAddress = newDetailAddr,
//            phone         = newPhone,
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
        return toUserResponse(saved)
    }


    suspend fun withdraw(user: UserEntity) {
        val existing = userRepository.findById(user.id)
            ?: throw UsernameNotFoundException("User not found: ${user.username}")

        val now = OffsetDateTime.now()

        // soft delete 처리
        val deleted = existing.copy(
            deletedAt = now,
            deletedId = user.id
        )
        userRepository.save(deleted)
    }


    suspend fun registerPhotos(user: UserEntity, files: List<FilePart>?) : UserResponse {
        if (files.isNullOrEmpty()) {
            throw BusinessValidationException(mapOf("file" to "이미지가 필요합니다."))
        }

        files.forEach { file ->
            // 1) 에셋 업로드
            val asset = assetService.uploadImage(file, user.id, AssetCategory.USER_PHOTO)

            // 2) user_photos row 생성
            val photoEntity = UserPhotosEntity(
                userId = user.id,
                uid = asset.uid
            )

            userPhotosRepository.save(photoEntity)
        }

        return toUserResponse(user)
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

    suspend fun myReferralList(userId: Long): List<ReferralSummary> {
        val referrals = inviteService.findMyReferralList(userId)
        if (referrals.isEmpty()) return emptyList()

        val (ids, dates) = referrals.unzip()
        val users = userRepository.findSummaryByIds(ids)

        // id -> referredAt 맵핑
        val referredAtMap = referrals.toMap()

        return users.map { user ->
            ReferralSummary(
                user = user,
                referredAt = referredAtMap[user.id]!!
            )
        }
    }



}
