package kr.jiasoft.hiteen.feature.report.infra

import kr.jiasoft.hiteen.feature.report.domain.ReportEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportRepository : CoroutineCrudRepository<ReportEntity, Long> {

    suspend fun findAllByUserId(userId: Long): List<ReportEntity>

    suspend fun findAllByTargetId(targetId: Long): List<ReportEntity>
}