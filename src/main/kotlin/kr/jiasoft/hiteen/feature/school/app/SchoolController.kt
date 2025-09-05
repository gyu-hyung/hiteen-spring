package kr.jiasoft.hiteen.feature.school.app

import kotlinx.coroutines.flow.toList
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

    /**
     * 학교 정보 조회 (검색)
     *
     * @param keyword 학교 이름 키워드 (없으면 전체 반환)
     */
    @GetMapping
    suspend fun getSchools(
        @RequestParam(required = false) keyword: String?
    ): List<SchoolEntity> {
        return if (keyword.isNullOrBlank()) {
            schoolRepository.findAll().toList()
        } else {
            schoolRepository.findByNameContaining(keyword).toList()
        }
    }



}