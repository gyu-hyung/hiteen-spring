package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.admin.dto.AdminIdOnlyRequest
import kr.jiasoft.hiteen.admin.dto.AdminLevelResponse
import kr.jiasoft.hiteen.admin.dto.AdminLevelSaveRequest
import kr.jiasoft.hiteen.admin.infra.AdminLevelRepository
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.feature.level.domain.TierEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/level")
class AdminLevelController(
    private val adminLevelRepository: AdminLevelRepository,
) {
    // 레벨 목록
    @GetMapping("/levels")
    suspend fun getLevels(
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
    ): ResponseEntity<ApiResult<List<AdminLevelResponse>>> {
        val data = adminLevelRepository.listLevels(searchType, search).toList()

        return success(data)
    }

    // 레벨정보 등록/수정
    @PostMapping("/save")
    suspend fun saveLevel(
        @Parameter req: AdminLevelSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<Any>> {
        if (req.tierCode.isBlank()) {
            return failure("레벨코드를 입력해 주세요.")
        }
        if (req.tierNameKr.isBlank()) {
            return failure("레벨명을 입력해 주세요.")
        }
        if (req.level < 1) {
            return failure("레벨번호는 1 이상이어야 합니다.")
        }
        if (req.divisionNo < 1) {
            return failure("세부번호는 1 이상이어야 합니다.")
        }
        if (req.rankOrder < 1) {
            return failure("전체순서는 1 이상이어야 합니다.")
        }
        if (req.minPoints < 0) {
            return failure("최소 포인트를 입력하세요.")
        }
        if (req.maxPoints < 1) {
            return failure("최대 포인트를 입력하세요.")
        }
        if (req.minPoints >= req.maxPoints) {
            return failure("최대 포인트는 최소 포인트보다 커야 합니다.")
        }

        val mode = req.mode ?: "add"
        val dateTime = OffsetDateTime.now()

        val result = if (mode == "edit") {
            // 수정
            val level = req.id?.let { adminLevelRepository.findById(it) }
                ?: return failure("존재하지 않는 레벨입니다(${req.id})")

            val data = level.copy(
                tierCode = req.tierCode,
                tierNameKr = req.tierNameKr,
                level = req.level,
                divisionNo = req.divisionNo,
                rankOrder = req.rankOrder,
                status = req.status,
                minPoints = req.minPoints,
                maxPoints = req.maxPoints,
                updatedAt = dateTime,
            )

            adminLevelRepository.save(data)
        } else {
            // 등록
            val data = TierEntity(
                tierCode = req.tierCode,
                tierNameKr = req.tierNameKr,
                level = req.level,
                divisionNo = req.divisionNo,
                rankOrder = req.rankOrder,
                status = req.status,
                minPoints = req.minPoints,
                maxPoints = req.maxPoints,
                uid = UUID.randomUUID(),
                createdAt = dateTime,
                updatedAt = dateTime,
            )

            adminLevelRepository.save(data)
        }

        val message = if (mode == "add") {
            "레벨이 등록되었습니다."
        } else {
            "레벨정보가 변경되었습니다."
        }

        return success(result, message)
    }

    /**
     * 레벨정보 삭제 (Soft Delete)
     */
    @PostMapping("/delete")
    suspend fun deleteLevel(
        @ModelAttribute req: AdminIdOnlyRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {
        val id = req.id

        val level = adminLevelRepository.findById(id)
            ?: return failure("존재하지 않는 레벨입니다(${id})")

        val countUsers = adminLevelRepository.countLevelUsers(level.id)
        if (countUsers > 0) {
            return failure("회원이 존재하여 삭제할 수 없습니다.")
        }

        val data = level.copy(
            deletedAt = OffsetDateTime.now(),
        )

        adminLevelRepository.save(data)

        return success(level, "레벨이 삭제되었습니다.")
    }
}