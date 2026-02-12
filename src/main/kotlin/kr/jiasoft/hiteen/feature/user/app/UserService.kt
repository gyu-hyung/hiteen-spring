package kr.jiasoft.hiteen.feature.user.app

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.services.AdminSchoolService
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.common.helpers.SchoolYearHelper
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.app.event.AssetThumbnailPrecreateRequestedEvent
import kr.jiasoft.hiteen.feature.asset.domain.AssetCategory
import kr.jiasoft.hiteen.feature.auth.dto.JwtResponse
import kr.jiasoft.hiteen.feature.auth.infra.JwtProvider
import kr.jiasoft.hiteen.feature.board.infra.BoardCommentRepository
import kr.jiasoft.hiteen.feature.board.infra.BoardRepository
import kr.jiasoft.hiteen.feature.interest.domain.InterestUserEntity
import kr.jiasoft.hiteen.feature.interest.infra.InterestRepository
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.invite.app.InviteService
import kr.jiasoft.hiteen.feature.level.domain.TierCode
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.poll.infra.PollCommentRepository
import kr.jiasoft.hiteen.feature.poll.infra.PollUserRepository
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.relationship.domain.FollowStatus
import kr.jiasoft.hiteen.feature.relationship.domain.FriendStatus
import kr.jiasoft.hiteen.feature.relationship.dto.RelationshipCounts
import kr.jiasoft.hiteen.feature.relationship.infra.FollowRepository
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolClassesRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import kr.jiasoft.hiteen.feature.user.dto.ReferralSummary
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserResponse
import kr.jiasoft.hiteen.feature.user.dto.UserResponseIncludes
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
import org.springframework.context.ApplicationEventPublisher
import java.time.OffsetDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import kr.jiasoft.hiteen.feature.asset.domain.ThumbnailMode
import kr.jiasoft.hiteen.feature.asset.dto.AssetResponse
import kr.jiasoft.hiteen.feature.cash.app.CashService
import kr.jiasoft.hiteen.feature.cash.domain.CashPolicy

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
    private val schoolClassesRepository: SchoolClassesRepository,
    private val interestUserRepository: InterestUserRepository,
    private val boardRepository: BoardRepository,
    private val pollUserRepository: PollUserRepository,
    private val boardCommentRepository: BoardCommentRepository,
    private val pollCommentRepository: PollCommentRepository,
    private val inviteService: InviteService,
    private val tierRepository: TierRepository,
    private val pointService: PointService,
    private val interestRepository: InterestRepository,
//    private val interestUserService: InterestUserService,
    private val eventPublisher: ApplicationEventPublisher,

    private val cashService: CashService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

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


    private suspend fun toUserResponse(
        targetUser: UserEntity,
        currentUserId: Long? = null,
        includes: UserResponseIncludes = UserResponseIncludes.full(),
    ): UserResponse {
        val school = if (includes.school) {
            targetUser.schoolId?.let { id -> schoolRepository.findById(id) }
        } else null

        val classes = if (includes.schoolClass) {
            targetUser.classId?.let { id -> schoolClassesRepository.findById(id) }
        } else null

        val tier = if (includes.tier) {
            tierRepository.findById(targetUser.tierId)
        } else null

        val interests = if (includes.interests) {
            interestUserRepository.getInterestResponseById(id = null, userId = targetUser.id).toList()
        } else null

        val relationshipCounts = if (includes.relationshipCounts) {
            RelationshipCounts(
                postCount = boardRepository.countByCreatedIdAndDeletedAtIsNull(targetUser.id),
                voteCount = pollUserRepository.countByUserIdAndDeletedAtIsNull(targetUser.id),
                boardCommentCount = boardCommentRepository.countByCreatedIdAndDeletedAtIsNull(targetUser.id),
                pollCommentCount = pollCommentRepository.countByCreatedIdAndDeletedAtIsNull(targetUser.id),
                friendCount = friendRepository.countFriendship(targetUser.id),
                followerCount = followRepository.countByFollowIdAndStatus(targetUser.id, FollowStatus.ACCEPTED.name),
                followingCount = followRepository.countByUserIdAndStatus(targetUser.id, FollowStatus.ACCEPTED.name),
            )
        } else null

        val photos = if (includes.photos) getPhotosById(targetUser.id) else null

        val friendStatus =
            if (includes.relationshipFlags && currentUserId != null) friendRepository.findStatusFriend(currentUserId, targetUser.id)
            else false
        val followStatus =
            if (includes.relationshipFlags && currentUserId != null) followRepository.findStatusFollow(currentUserId, targetUser.id)
            else false

        val isFriend = friendStatus == FriendStatus.ACCEPTED.name
        val isFriendRequested = friendStatus == FriendStatus.PENDING.name

        val isFollowed = followStatus == FollowStatus.ACCEPTED.name
        val isFollowedRequested = followStatus == FollowStatus.PENDING.name

        return UserResponse.from(
            entity = targetUser,
            school = school,
            classes = classes,
            tier = tier,
            interests = interests,
            relationshipCounts = relationshipCounts,
            photos = photos,
            isFriend = isFriend,
            isFollowed = isFollowed,
            isFriendRequested = isFriendRequested,
            isFollowedRequested = isFollowedRequested,
        )
    }

    suspend fun findUserResponse(username: String): UserResponse {
        val targetUser = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return toUserResponse(targetUser)
    }

    suspend fun findUserResponse(targetId: Long, currentUserId: Long? = null): UserResponse {
        val targetUser = userRepository.findById(targetId)
            ?: throw UsernameNotFoundException("User not found: $targetId")

        return toUserResponse(targetUser, currentUserId)
    }

    suspend fun findUserResponse(targetUid: UUID, currentUserId: Long? = null): UserResponse {
        val targetUser = userRepository.findByUid(targetUid.toString())
            ?: throw UsernameNotFoundException("User not found: $targetUid")

        return toUserResponse(targetUser, currentUserId)
    }

    /**
     * 선택적으로 연관 도메인을 포함한 UserResponse 조회
     */
    suspend fun findUserResponse(
        targetId: Long,
        currentUserId: Long? = null,
        includes: UserResponseIncludes,
    ): UserResponse {
        val targetUser = userRepository.findById(targetId)
            ?: throw UsernameNotFoundException("User not found: $targetId")

        return toUserResponse(targetUser, currentUserId, includes)
    }

//    @Cacheable(cacheNames = ["userSummary"], key = "#userId")
    suspend fun findUserSummary(userId: Long): UserSummary {
        return userRepository.findSummaryInfoById(userId)
    }

    suspend fun findUserSummaryByIds(userIds: List<Long>): List<UserSummary> {
        if (userIds.isEmpty()) return emptyList()
        return userRepository.findSummaryByIds(userIds)
    }

    suspend fun findUserResponseByIds(
        targetIds: List<Long>,
        currentUserId: Long? = null,
        includes: UserResponseIncludes = UserResponseIncludes.full()
    ): List<UserResponse> {
        if (targetIds.isEmpty()) return emptyList()
        val targetUsers = userRepository.findAllById(targetIds).toList()
        if (targetUsers.isEmpty()) return emptyList()

        // 1) 학교/반/티어 정보 일괄 조회
        val schoolIds = targetUsers.mapNotNull { it.schoolId }.distinct()
        val classIds = targetUsers.mapNotNull { it.classId }.distinct()
        val tierIds = targetUsers.map { it.tierId }.distinct()

        val schoolMap = if (includes.school && schoolIds.isNotEmpty())
            schoolRepository.findAllById(schoolIds).toList().associateBy { it.id }
        else emptyMap()

        val classMap = if (includes.schoolClass && classIds.isNotEmpty())
            schoolClassesRepository.findAllById(classIds).toList().associateBy { it.id }
        else emptyMap()

        val tierMap = if (includes.tier && tierIds.isNotEmpty())
            tierRepository.findAllById(tierIds).toList().associateBy { it.id }
        else emptyMap()

        // 2) 관심사 일괄 조회
        val interestMap = if (includes.interests)
            interestUserRepository.getInterestResponseByUserIds(targetIds).toList().groupBy { it.userId }
        else emptyMap()

        // 3) 관계 카운트 정보 일괄 조회
        val postCounts = if (includes.relationshipCounts) boardRepository.countBulkByCreatedIdIn(targetIds).toList().associate { it.id to it.count } else emptyMap()
        val voteCounts = if (includes.relationshipCounts) pollUserRepository.countBulkByUserIdIn(targetIds).toList().associate { it.id to it.count } else emptyMap()
        val bCommentCounts = if (includes.relationshipCounts) boardCommentRepository.countBulkByCreatedIdIn(targetIds).toList().associate { it.id to it.count } else emptyMap()
        val pCommentCounts = if (includes.relationshipCounts) pollCommentRepository.countBulkByCreatedIdIn(targetIds).toList().associate { it.id to it.count } else emptyMap()
        val friendCounts = if (includes.relationshipCounts) friendRepository.countBulkFriendshipIn(targetIds).toList().associate { it.id to it.count } else emptyMap()
        val followerCounts = if (includes.relationshipCounts) followRepository.countBulkFollowersIn(targetIds, FollowStatus.ACCEPTED.name).toList().associate { it.id to it.count } else emptyMap()
        val followingCounts = if (includes.relationshipCounts) followRepository.countBulkFollowingIn(targetIds, FollowStatus.ACCEPTED.name).toList().associate { it.id to it.count } else emptyMap()

        // 4) 사진 일괄 조회
        val photoMap = if (includes.photos)
            userPhotosRepository.findAllByUserIdIn(targetIds).toList().groupBy { it.userId }
        else emptyMap()

        // 5) 관계 플래그 일괄 조회 (로그인 시)
        val friendStatusMap = if (includes.relationshipFlags && currentUserId != null)
            friendRepository.findBulkStatusFriendIn(currentUserId, targetIds).toList().associate { it.id to it.countStr }
        else emptyMap()

        val followStatusMap = if (includes.relationshipFlags && currentUserId != null)
            followRepository.findBulkStatusFollowIn(currentUserId, targetIds).toList().associate { it.id to it.countStr }
        else emptyMap()

        // 6) 결과 조립
        return targetUsers.map { user ->
            val relationshipCounts = if (includes.relationshipCounts) {
                RelationshipCounts(
                    postCount = postCounts[user.id] ?: 0,
                    voteCount = voteCounts[user.id] ?: 0,
                    boardCommentCount = bCommentCounts[user.id] ?: 0,
                    pollCommentCount = pCommentCounts[user.id] ?: 0,
                    friendCount = friendCounts[user.id] ?: 0,
                    followerCount = followerCounts[user.id] ?: 0,
                    followingCount = followingCounts[user.id] ?: 0
                )
            } else null

            val fStatus = friendStatusMap[user.id]
            val flStatus = followStatusMap[user.id]

            UserResponse.from(
                entity = user,
                school = schoolMap[user.schoolId],
                classes = classMap[user.classId],
                tier = tierMap[user.tierId],
                interests = interestMap[user.id],
                relationshipCounts = relationshipCounts,
                photos = photoMap[user.id],
                isFriend = fStatus == FriendStatus.ACCEPTED.name,
                isFriendRequested = fStatus == FriendStatus.PENDING.name,
                isFollowed = flStatus == FollowStatus.ACCEPTED.name,
                isFollowedRequested = flStatus == FollowStatus.PENDING.name
            )
        }
    }

    @Cacheable(cacheNames = ["userEntity"], key = "#id")
    suspend fun findByUsername(id: Long): UserEntity {
        println("✅✅✅✅✅✅✅✅✅✅✅✅✅ VVVV")
        val user = userRepository.findById(id)
        println("✅✅✅✅✅✅✅✅✅✅✅✅✅ AAAA ")
        return user
            ?: throw UsernameNotFoundException("User not found: $id")
    }


    /**
     * 회원 가입
     * 재가입 규칙: 탈퇴 후 같은 번호로 30일 이전에 재가입 가능
     * dev-allow-always -> false 면 언제나 새로 가입
     * */
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

            // =========================================================
            //                       기본 관심사 init
            // =========================================================
            // 기본 관심사
//            interestUserService.initDefaultInterests(updated)
            // 기본 옵션 키워드
            val defaultOptions = listOf("관심사", "남학생", "여학생", "동급생", "선배", "후배")

            // ① 현재 등록된 관심사 조회
            val existing = interestUserRepository.findByUserIdWithInterest(updated.id)
                .map { it.topic }
                .toSet()

            // ② 마스터 테이블에서 "추천옵션" 카테고리 중 기본 옵션에 해당하는 항목 조회
            val masterOptions = interestRepository.findByCategoryAndTopicIn("추천옵션", defaultOptions).toList()

            if (masterOptions.isEmpty()) {
                println("⚠️ 기본 관심사(추천옵션) 마스터 데이터가 존재하지 않습니다.")
//                return
            }

            // ③ 등록되지 않은 항목만 필터링
            val toInsert = masterOptions.filterNot { existing.contains(it.topic) }
            if (toInsert.isEmpty()) {
                println("✅ 기본 추천옵션 관심사가 이미 모두 등록되어 있습니다.")
//                return
            }

            // ④ interest_user 엔티티로 변환 후 저장
            toInsert.forEach { master ->
                interestUserRepository.save(
                    InterestUserEntity(
                        interestId = master.id,
                        userId = updated.id,
                    )
                )
            }

            println("🌱 ${updated.nickname} 기본 추천옵션 관심사 ${toInsert.size}개 등록 완료")

            // =========================================================
            //                       기본 관심사 init
            // =========================================================

            // 초대코드 생성
            inviteService.registerInviteCode(updated)
            // 초대코드로 가입 처리
            if (!inviteCode.isNullOrBlank()) {
                val inviterId = inviteService.handleInviteJoin(updated, inviteCode.trim())
                    ?: throw BusinessValidationException(mapOf("inviteCode" to "유효하지 않은 초대코드입니다."))

                // ✅ 초대자에게 푸시 알림 (중복 조회 없음)
                eventPublisher.publishEvent(
                    PushSendRequestedEvent(
                        userIds = listOf(inviterId),
                        actorUserId = updated.id,
                        templateData = PushTemplate.INVITE_CODE_JOINED.buildPushData(
                            "nickname" to updated.nickname,
                        ),
                        extraData = mapOf(
                            "joinUserId" to updated.id.toString(),
                        ),
                    )
                )
            }

            val responseUser = userRepository.findById(updated.id)!!.let {
                UserResponse.from(it, school, null, tier)
            }

            // JWT
            val (access, refresh) = jwtProvider.generateTokens(updated.username)
            // 포인트 지급
            pointService.applyPolicy(updated.id, PointPolicy.SIGNUP)

            cashService.applyPolicy(updated.id, CashPolicy.SIGNUP)

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

        var newAssetUid: UUID? = existing.assetUid
        var oldAssetUidToDelete: UUID? = null


        // 프로필 이미지 제거 처리
        if(param.assetUid != null) {
            newAssetUid = null
            oldAssetUidToDelete = existing.assetUid
        }

        // 1) 파일 업로드 처리
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
        val newEmail        = param.email?.trim()?.takeIf { it.isNotEmpty() } ?: existing.email
        val newNickname     = param.nickname ?: existing.nickname
        val newPassword     = param.password?.let { encoder.encode(it) } ?: existing.password
        val newAddress      = param.address ?: existing.address
        val newDetailAddr   = param.detailAddress ?: existing.detailAddress
//        val newPhone       = param.phone ?: existing.phone
        val newMood         = param.mood ?: existing.mood
        val newMoodEmoji    = param.moodEmoji ?: existing.moodEmoji
        val newSchoolId     = param.schoolId ?: existing.schoolId
        // 학교가 바뀌면 기존 학급 정보는 무효이므로 classId 초기화
        // - param.classId가 명시되면 그 값을 사용
        // - param.classId가 없고 schoolId가 변경되면 null로 초기화
        // - 그 외에는 기존값 유지
        val newClassId      = param.classId ?: if (existing.schoolId != newSchoolId) null else existing.classId
        val newGrade        = param.grade ?: existing.grade
        val newGender       = param.gender ?: existing.gender
        val newBirthday     = param.birthday ?: existing.birthday
        val newProfileDecorationCode = param.profileDecorationCode ?: existing.profileDecorationCode
        val newLocationMode = param.locationMode ?: existing.locationMode

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

        // ✅ 학교 변경 30일 제한 정책
        val schoolChanged = existing.schoolId != newSchoolId
        val newYear = if(schoolChanged) SchoolYearHelper.getCurrentSchoolYear() else existing.year

        val newSchoolUpdatedAt = if (schoolChanged) {
            val lastChangedAt = existing.schoolUpdatedAt
            if (lastChangedAt != null) {
                val nextAllowedAt = lastChangedAt.plusDays(30)
                if (OffsetDateTime.now().isBefore(nextAllowedAt)) {
                    throw BusinessValidationException(
                        mapOf(
                            "schoolId" to "학교는 변경 후 30일 동안 수정할 수 없습니다. (다음 변경 가능: $nextAllowedAt)"
                        )
                    )
                }
            }
            OffsetDateTime.now()
        } else {
            existing.schoolUpdatedAt
        }

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
            schoolUpdatedAt = newSchoolUpdatedAt,
            classId       = newClassId,
            grade         = newGrade,
            gender        = newGender,
            birthday      = newBirthday,
            profileDecorationCode = newProfileDecorationCode,
            locationMode  = newLocationMode,
            year          = newYear,
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
        val t0 = System.nanoTime()

        if (files.isNullOrEmpty()) {
            throw BusinessValidationException(mapOf("file" to "이미지가 필요합니다."))
        }

        val existingCount = userPhotosRepository.countByUserId(user.id)
        val imageCount = existingCount + files.size
        //최소 3장
        if (imageCount < 3) {
            throw BusinessValidationException(mapOf("file" to "최소 사진 3장은 꼭 등록해야 돼"))
        }
        //최대 6장
        if (imageCount > 6) {
            throw BusinessValidationException(mapOf("file" to "사진은 최대 6장까지 등록할 수 있어"))
        }

        log.debug(
            "✅✅ [registerPhotos] start userId={} existingCount={} uploadCount={} filenames={}",
            user.id,
            existingCount,
            files.size,
            files.map { it.filename() }
        )

        val tUploadStart = System.nanoTime()
        val uploaded = mutableListOf<AssetResponse>()
        val tThumbStart = System.nanoTime()

        // ✅ 파일 1개 업로드(DB 저장) 완료될 때마다 즉시 썸네일 이벤트 발행
        for (f in files) {
            val a = assetService.uploadImage(f, user.id, AssetCategory.USER_PHOTO)
            uploaded.add(a)
            eventPublisher.publishEvent(
                AssetThumbnailPrecreateRequestedEvent(
                    assetUids = listOf(a.uid),
                    width = 780,
                    height = 966,
                    mode = ThumbnailMode.COVER,
                    requestedByUserId = user.id,
                )
            )
        }

        val tUploadMs = (System.nanoTime() - tUploadStart) / 1_000_000
        val tThumbMs = (System.nanoTime() - tThumbStart) / 1_000_000

        log.debug(
            "✅✅ [registerPhotos] upload done userId={} uploadedCount={} elapsedMs={} assetUids={}",
            user.id,
            uploaded.size,
            tUploadMs,
            uploaded.map { it.uid }
        )

        val tDbStart = System.nanoTime()
        uploaded.forEach { asset ->
            val photoEntity = UserPhotosEntity(
                userId = user.id,
                uid = asset.uid
            )
            userPhotosRepository.save(photoEntity)
        }
        val tDbMs = (System.nanoTime() - tDbStart) / 1_000_000

        val totalMs = (System.nanoTime() - t0) / 1_000_000
        log.debug(
            "✅✅ [registerPhotos] done userId={} uploadMs={} dbMs={} thumbMs={} totalMs={}",
            user.id,
            tUploadMs,
            tDbMs,
            tThumbMs,
            totalMs
        )

        return toUserResponse(user)
    }


    /** 프로필 이미지 단건 등록 */
    suspend fun registerPhotoSingle(user: UserEntity, file: FilePart?): UserResponse {
        if (file == null) {
            throw BusinessValidationException(mapOf("file" to "이미지가 필요합니다."))
        }

//        val existingCount = userPhotosRepository.countByUserId(user.id).toInt()
//        val imageCount = existingCount + 1

        // 정책 유지: 최소 3장 / 최대 6장
//        if (imageCount > 3) {
//            throw BusinessValidationException(mapOf("file" to "최소 사진 3장은 꼭 등록해야 돼"))
//        }
//        if (imageCount > 6) {
//            throw BusinessValidationException(mapOf("file" to "사진은 최대 6장 까지 등록할 수 있어"))
//        }

        val t0 = System.nanoTime()
        log.debug(
            "✅ [registerPhotoSingle] start userId={} existingCount={} filename={}",
            user.id,
//            existingCount,
            file.filename()
        )

        val tUploadStart = System.nanoTime()
        val uploaded = assetService.uploadImage(file, user.id, AssetCategory.USER_PHOTO)
        val uploadMs = (System.nanoTime() - tUploadStart) / 1_000_000

        val tDbStart = System.nanoTime()
        userPhotosRepository.save(
            UserPhotosEntity(
                userId = user.id,
                uid = uploaded.uid
            )
        )
        val dbMs = (System.nanoTime() - tDbStart) / 1_000_000

        val tThumbStart = System.nanoTime()
        eventPublisher.publishEvent(
            AssetThumbnailPrecreateRequestedEvent(
                assetUids = listOf(uploaded.uid),
                width = 780,
                height = 966,
                mode = ThumbnailMode.COVER,
                requestedByUserId = user.id,
            )
        )
        val thumbMs = (System.nanoTime() - tThumbStart) / 1_000_000

        val totalMs = (System.nanoTime() - t0) / 1_000_000
        log.debug(
            "✅ [registerPhotoSingle] done userId={} uploadMs={} dbMs={} thumbMs={} totalMs={} assetUid={}",
            user.id,
            uploadMs,
            dbMs,
            thumbMs,
            totalMs,
            uploaded.uid
        )

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
        val flow = userPhotosRepository.findByUserId(userId).toList()
        return flow
    }


    suspend fun getPhotos(userUid: String): List<UserPhotosEntity> {
        val userEntity = userRepository.findByUid(userUid)
            ?: throw BusinessValidationException(mapOf("user" to "존재하지 않는 사용자입니다."))

        return userPhotosRepository.findByUserId(userEntity.id).toList()
    }

    suspend fun myReferralList(userId: Long): List<ReferralSummary> {
        val referrals = inviteService.findMyReferralList(userId)
        if (referrals.isEmpty()) return emptyList()

        val (ids, _) = referrals.unzip()
        val users = userRepository.findSummaryByIds(ids)

        // id -> referredAt 매핑
        val referredAtMap = referrals.toMap()

        return users.map { user ->
            ReferralSummary(
                user = user,
                referredAt = referredAtMap[user.id]!!
            )
        }
    }

    /** 틴프로필 삭제 (사진, 관심사 전체 삭제) */
    suspend fun deleteTeenProfile(user: UserEntity) {
        // 1) 추가 사진 삭제
        val photos = userPhotosRepository.findByUserId(user.id).toList() ?: emptyList()
        photos.forEach { photo ->
            try {
                assetService.softDelete(photo.uid, user.id)
            } catch (_: Throwable) {}
        }
        userPhotosRepository.deleteByUserId(user.id)

        // 2) 관심사 삭제
        interestUserRepository.deleteByUserId(user.id)
    }

}
