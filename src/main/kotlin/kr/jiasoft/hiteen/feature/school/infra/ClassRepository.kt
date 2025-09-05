package kr.jiasoft.hiteen.feature.school.infra

import kr.jiasoft.hiteen.feature.school.domain.ClassEntity
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ClassRepository : ReactiveCrudRepository<ClassEntity, Long> {
    suspend fun findBySchoolId(schoolId: Long): List<ClassEntity>
}