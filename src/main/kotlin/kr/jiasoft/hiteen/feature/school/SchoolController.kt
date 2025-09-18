package kr.jiasoft.hiteen.feature.school

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.springframework.web.bind.annotation.*

@Tag(name = "School", description = "학교 정보 조회 API")
@RestController
@RequestMapping("/api/school")
class SchoolController(
    private val schoolRepository: SchoolRepository
) {

    //TODO : 없는 학교 문의하기
    //TODO : HITEEN 사용중 유저 COUNT
    //TODO : 학교 주소 위경도 변환
    //TODO : 학교 최신정보 갱신 JOB
    @Operation(
        summary = "학교 정보 조회",
        description = "학교 이름 키워드 검색 및 커서 기반 페이지네이션으로 학교 목록을 조회합니다."
    )
    @GetMapping
    suspend fun getSchools(
        @Parameter(description = "학교 이름 키워드 (없으면 전체)") @RequestParam(required = false) keyword: String?,
        @Parameter(description = "마지막으로 조회한 학교 ID") @RequestParam(required = false) cursor: Long?,
        @Parameter(description = "페이지당 개수 (기본 30)") @RequestParam(required = false, defaultValue = "30") limit: Int
    ): ApiPageCursor<SchoolEntity> {
        val items: List<SchoolEntity> = if (keyword.isNullOrBlank()) {
            if (cursor == null) {
                schoolRepository.findFirstPage(limit).toList()
            } else {
                schoolRepository.findNextPage(cursor, limit).toList()
            }
        } else {
            if (cursor == null) {
                schoolRepository.findByNameContainingFirstPage(keyword, limit).toList()
            } else {
                schoolRepository.findByNameContainingNextPage(keyword, cursor, limit).toList()
            }
        }

        val nextCursor = items.lastOrNull()?.id?.toString()

        return ApiPageCursor(
            nextCursor = nextCursor,
            items = items,
            perPage = limit
        )
    }
}
