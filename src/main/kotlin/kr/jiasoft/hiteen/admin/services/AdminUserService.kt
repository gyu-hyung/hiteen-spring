package kr.jiasoft.hiteen.admin.services

import kr.jiasoft.hiteen.admin.dto.AdminUserResponse
import kr.jiasoft.hiteen.admin.dto.AdminUserSaveRequest
import kr.jiasoft.hiteen.admin.dto.AdminUserRoleUpdateRequest
import kr.jiasoft.hiteen.admin.dto.AdminUserRoleUpdateResponse
import kr.jiasoft.hiteen.admin.infra.AdminUserRepository
import kr.jiasoft.hiteen.feature.asset.app.AssetService
import kr.jiasoft.hiteen.feature.asset.infra.AssetRepository
import kr.jiasoft.hiteen.feature.interest.domain.InterestUserEntity
import kr.jiasoft.hiteen.feature.interest.infra.InterestUserRepository
import kr.jiasoft.hiteen.feature.level.infra.TierRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolClassesRepository
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import kr.jiasoft.hiteen.feature.user.infra.UserPhotosRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.time.OffsetDateTime
import java.util.UUID

@Service
class AdminUserService(
    private val adminUserRepository: AdminUserRepository,
    private val userService: UserService,

    private val schoolRepository: SchoolRepository,
    private val schoolClassesRepository: SchoolClassesRepository,
    private val tierRepository: TierRepository,

    private val interestUserRepository: InterestUserRepository,
    private val userPhotosRepository: UserPhotosRepository,
    private val assetRepository: AssetRepository,
    private val assetService: AssetService,

    private val txOperator: TransactionalOperator,
) {

    suspend fun updateRole(request: AdminUserRoleUpdateRequest, admin: UserEntity): AdminUserRoleUpdateResponse {
        val normalizedRole = request.role.trim().uppercase()
        require(normalizedRole == "ADMIN" || normalizedRole == "USER") { "role must be ADMIN or USER" }

        val targetUid = try { UUID.fromString(request.uid) } catch (_: Exception) {
            throw IllegalArgumentException("invalid uid")
        }

        if (admin.uid == targetUid) {
            throw IllegalArgumentException("본인 계정의 role은 변경할 수 없습니다.")
        }

        return txOperator.executeAndAwait {
            val target = adminUserRepository.findByUid(targetUid)
                ?: throw IllegalArgumentException("존재하지 않는 사용자입니다.")

            val now = OffsetDateTime.now()
            val saved = adminUserRepository.save(
                target.copy(
                    role = normalizedRole,
                    updatedId = admin.id,
                    updatedAt = now,
                )
            )

            AdminUserRoleUpdateResponse(
                id = saved.id,
                uid = saved.uid.toString(),
                role = saved.role,
                updatedId = saved.updatedId,
                updatedAt = saved.updatedAt,
            )
        }
    }

    suspend fun save(request: AdminUserSaveRequest, admin: UserEntity): AdminUserResponse {
        return txOperator.executeAndAwait {
            val savedUser = if (request.id == null) {
                createUser(request, admin)
            } else {
                updateUser(request, admin)
            }

            if (request.interestIds != null) {
                syncInterests(savedUser.id, request.interestIds)
            }
            if (request.photoUids != null) {
                syncPhotos(savedUser.id, request.photoUids)
            }

            val adminRow = adminUserRepository.findResponseByUid(savedUser.uid)
                ?: throw IllegalStateException("저장된 사용자를 다시 조회할 수 없습니다.")
            val userResponse = userService.findUserResponse(adminRow.id)
            AdminUserResponse.from(adminRow, userResponse)
        }
    }

    private suspend fun createUser(request: AdminUserSaveRequest, admin: UserEntity): UserEntity {
        validateFKs(request)

        val now = OffsetDateTime.now()

        val newUser = UserEntity(
            id = 0,
            username = request.username ?: "",
            email = request.email,
            nickname = request.nickname ?: "",
            phone = request.phone ?: "",
            role = request.role ?: "USER",
            gender = request.gender,
            birthday = request.birthday,
            address = request.address,
            detailAddress = request.detailAddress,
            mood = request.mood,
            moodEmoji = request.moodEmoji,
            mbti = request.mbti,
            assetUid = request.assetUid,
            schoolId = request.schoolId,
            classId = request.classId,
            tierId = request.tierId ?: 0,
            year = request.year,
            createdId = admin.id,
            createdAt = now,
            updatedId = admin.id,
            updatedAt = now,
        )

        return try {
            adminUserRepository.save(newUser)
        } catch (_: DataIntegrityViolationException) {
            throw IllegalArgumentException("사용자 생성에 실패했습니다. (중복/제약조건 위반)")
        }
    }

    private suspend fun updateUser(request: AdminUserSaveRequest, admin: UserEntity): UserEntity {
        val existing = adminUserRepository.findById(request.id!!)
            ?: throw UsernameNotFoundException("User not found: ${request.id}")

        validateFKs(request)

        val now = OffsetDateTime.now()

        // UserService.updateUser의 프로필 이미지 처리와 동일한 정책:
        // - assetUid가 들어오면 "프로필 이미지 제거"로 간주 (newAssetUid = null)
        // - (추후) 파일 업로드를 지원한다면 old -> softDelete 필요
        var newAssetUid: UUID? = existing.assetUid
        var oldAssetUidToDelete: UUID? = null
        if (request.assetUid != null) {
            // "제거" 요청으로 해석
            newAssetUid = null
            oldAssetUidToDelete = existing.assetUid
        }

        val updated = existing.copy(
            username = request.username ?: existing.username,
            email = request.email ?: existing.email,
            nickname = request.nickname ?: existing.nickname,
            phone = request.phone ?: existing.phone,
            role = request.role ?: existing.role,
            gender = request.gender ?: existing.gender,
            birthday = request.birthday ?: existing.birthday,
            address = request.address ?: existing.address,
            detailAddress = request.detailAddress ?: existing.detailAddress,
            mood = request.mood ?: existing.mood,
            moodEmoji = request.moodEmoji ?: existing.moodEmoji,
            mbti = request.mbti ?: existing.mbti,
            assetUid = newAssetUid,
            locationMode = request.locationMode ?: existing.locationMode,
            schoolId = request.schoolId ?: existing.schoolId,
            classId = request.classId ?: existing.classId,
            tierId = request.tierId ?: existing.tierId,
            year = request.year ?: existing.year,

            updatedAt = now,
            updatedId = admin.id,
        )

        val saved = try {
            adminUserRepository.save(updated)
        } catch (_: DataIntegrityViolationException) {
            throw IllegalArgumentException("사용자 수정에 실패했습니다. (중복/제약조건 위반)")
        }

        // 기존 프로필 이미지 소프트 삭제 (UserService.updateUser와 동일 방식)
        if (oldAssetUidToDelete != null) {
            try { assetService.softDelete(oldAssetUidToDelete, admin.id) } catch (_: Throwable) {}
        }

        return saved
    }

    private suspend fun validateFKs(request: AdminUserSaveRequest) {
        request.schoolId?.let { schoolId ->
            if (schoolRepository.findById(schoolId) == null) {
                throw IllegalArgumentException("존재하지 않는 schoolId 입니다.")
            }
        }

        request.classId?.let { classId ->
            val schoolClass = schoolClassesRepository.findById(classId)
                ?: throw IllegalArgumentException("존재하지 않는 classId 입니다.")

            request.schoolId?.let { schoolId ->
                if (schoolClass.schoolId != schoolId) {
                    throw IllegalArgumentException("classId가 schoolId에 속하지 않습니다.")
                }
            }
        }

        request.tierId?.let { tierId ->
            val tier = tierRepository.findById(tierId)
                ?: throw IllegalArgumentException("존재하지 않는 tierId 입니다.")
            if (tier.status != "ACTIVE") {
                throw IllegalArgumentException("ACTIVE 상태의 tier만 설정할 수 있습니다.")
            }
        }

        request.assetUid?.let { assetUid ->
            if (assetRepository.findByUid(assetUid) == null) {
                throw IllegalArgumentException("존재하지 않는 assetUid 입니다.")
            }
        }
    }

    private suspend fun syncInterests(userId: Long, interestIds: List<Long>) {
        interestUserRepository.deleteByUserId(userId)
        val unique = interestIds.distinct()
        unique.forEach { interestId ->
            interestUserRepository.save(
                InterestUserEntity(
                    interestId = interestId,
                    userId = userId,
                )
            )
        }
    }

    private suspend fun syncPhotos(userId: Long, photoUids: List<UUID>) {
        val unique = photoUids.distinct()

        // 검증: assets 존재 + 다른 user 매핑 여부
        unique.forEach { uid ->
            if (assetRepository.findByUid(uid) == null) {
                throw IllegalArgumentException("존재하지 않는 photo uid(asset) 입니다: $uid")
            }
            val existing = userPhotosRepository.findByUid(uid)
            if (existing != null && existing.userId != userId) {
                throw IllegalArgumentException("이미 다른 사용자에 등록된 photo uid 입니다: $uid")
            }
        }

        // 검증이 끝난 후 delete -> insert
        userPhotosRepository.deleteByUserId(userId)
        unique.forEach { uid ->
            userPhotosRepository.save(UserPhotosEntity(userId = userId, uid = uid))
        }
    }
}
