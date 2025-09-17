package kr.jiasoft.hiteen.feature.school

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.common.dto.ApiPageCursor
import kr.jiasoft.hiteen.feature.school.domain.SchoolEntity
import kr.jiasoft.hiteen.feature.school.infra.SchoolRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/school")
class SchoolController(
    private val schoolRepository: SchoolRepository
) {

    /** TODO : 없는 학교 문의하기 */

    /**
     * 학교 정보 조회 (검색 + 커서 기반 페이지네이션)
     * TODO : HITEEN 사용중 유저 COUNT
     * @param keyword 학교 이름 키워드 (없으면 전체 반환)
     * @param cursor  마지막으로 조회한 학교 ID (없으면 처음부터)
     * @param limit   페이지당 개수 (기본 30)
     */
    @GetMapping
    suspend fun getSchools(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(required = false, defaultValue = "30") limit: Int
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