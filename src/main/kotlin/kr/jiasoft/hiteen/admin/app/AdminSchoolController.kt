package kr.jiasoft.hiteen.admin.app

import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import kr.jiasoft.hiteen.admin.dto.AdminIdOnlyRequest
import kr.jiasoft.hiteen.admin.dto.AdminSchoolClassesResponse
import kr.jiasoft.hiteen.admin.dto.AdminSchoolSaveRequest
import kr.jiasoft.hiteen.admin.dto.AdminSchoolResponse
import kr.jiasoft.hiteen.admin.infra.AdminSchoolRepository
import kr.jiasoft.hiteen.admin.services.AdminSchoolService
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.extensions.failure
import kr.jiasoft.hiteen.common.extensions.success
import kr.jiasoft.hiteen.common.helpers.SchoolYearHelper
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin/school")
class AdminSchoolController(
    private val adminSchoolRepository: AdminSchoolRepository,
    private val schoolService: AdminSchoolService,
) {
    // 학교 목록
    @GetMapping("/schools")
    suspend fun getSchools(
        @RequestParam sido: String? = null,
        @RequestParam type: Int? = null,
        @RequestParam searchType: String? = null,
        @RequestParam search: String? = null,
        @RequestParam page: Int = 1,
        @RequestParam size: Int = 10
    ): ResponseEntity<ApiResult<ApiPage<AdminSchoolResponse>>> {
        val data = schoolService.listSchools(
            sido, type, searchType, search, page, size
        )

        return success(data)
    }

    // 학급 목록
    @GetMapping("/classes")
    suspend fun getClasses(
        @RequestParam schoolId: Long = 0,
        @RequestParam year: Int,
    ): ResponseEntity<ApiResult<List<AdminSchoolClassesResponse>>> {
        if (schoolId < 1) {
            return failure("학교구분을 선택하세요.")
        }

        var schoolYear = year
        if (year < 2025) {
            schoolYear = SchoolYearHelper.getCurrentSchoolYear()
        }

        val data = schoolService.listClasses(schoolId, schoolYear)

        return success(data)
    }

    // 학교정보 등록/수정
    @PostMapping("/save")
    suspend fun saveSchool(
        @Valid @ModelAttribute req: AdminSchoolSaveRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ) : ResponseEntity<ApiResult<Any>> {
        val mode = req.mode ?: "add"

        val result = if (mode == "edit") {
            // 수정
            val school = req.id?.let { adminSchoolRepository.findById(it) }
                ?: return failure("존재하지 않는 학교입니다(${req.id})")

            val data = school.copy(
                name = req.name,
                sido = req.sido,
                sidoName = req.sidoName,
                type = req.type,
                typeName = req.typeName,
                zipcode = req.zipcode,
                address = req.address,
                latitude = req.latitude,
                longitude = req.longitude,
                foundDate = req.foundDate,
                updatedId = user.id,
                updatedAt = LocalDateTime.now(),
                modified = 1,
            )

            adminSchoolRepository.save(data)
        } else {
            // 등록
            val maxNum = adminSchoolRepository.findMaxSchoolCodeNumber() ?: 0
            val code = "H" + String.format("%06d", maxNum + 1)

            val data = SchoolEntity(
                code = code,
                name = req.name,
                sido = req.sido,
                sidoName = req.sidoName,
                type = req.type,
                typeName = req.typeName,
                zipcode = req.zipcode,
                address = req.address,
                latitude = req.latitude,
                longitude = req.longitude,
                foundDate = req.foundDate,
                modified = 1,
                createdId = user.id,
                createdAt = LocalDateTime.now(),
                updatedId = user.id,
                updatedAt = LocalDateTime.now(),
            )

            adminSchoolRepository.save(data)
        }

        val message = if (mode == "add") {
            "학교정보가 등록되었습니다."
        } else {
            "학교정보가 변경되었습니다."
        }

        return success(result, message)
    }

    /**
     * 학교정보 삭제 (Soft Delete)
     */
    @PostMapping("/delete")
    suspend fun deleteSchool(
        @Parameter(description = "학교 ID")
        @ModelAttribute req: AdminIdOnlyRequest,
        @AuthenticationPrincipal(expression = "user") user: UserEntity,
    ): ResponseEntity<ApiResult<Any>> {
        val id = req.id

        val school = adminSchoolRepository.findById(id)
            ?: return failure("존재하지 않는 학교입니다(${id})")

        val countUsers = adminSchoolRepository.countSchoolUsers(school.id)
        if (countUsers > 0) {
            return failure("학교의 회원이 존재하여 삭제할 수 없습니다.")
        }

        val data = school.copy(
            deletedId = user.id,
            deletedAt = LocalDateTime.now(),
            modified = 1,
        )

        adminSchoolRepository.save(data)

        return success(school, "학교정보가 삭제되었습니다.")
    }
}