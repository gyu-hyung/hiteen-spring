package kr.jiasoft.hiteen.feature.school.app

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.dto.SchoolDto
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "School", description = "학교 정보 조회 API")
@RestController
@RequestMapping("/api/school")
class SchoolController(
    private val schoolRepository: SchoolRepository
) {

    @Operation(
        summary = "학교 정보 조회",
        description = "학교 이름 키워드 검색 및 커서 기반 페이지네이션으로 학교 목록을 조회합니다."
    )
    @GetMapping
    suspend fun getSchools(
        keyword: String?,
        cursor: Long?,
        limit: Int?
    ): ApiPageCursor<SchoolDto> {
        val pageSize = limit ?: 20
        val entities = schoolRepository.findSchools(keyword, cursor, pageSize).toList()
        val items: List<SchoolDto> = entities.map {
            SchoolDto.from(it, schoolRepository.countMembersBySchoolId(it.id!!))
        }
        val nextCursor = entities.lastOrNull()?.id?.toString()

        return ApiPageCursor(
            nextCursor = nextCursor,
            items = items,
            perPage = pageSize
        )
    }

}