package kr.jiasoft.hiteen.feature.poll.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.poll.domain.PollTemplateEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollTemplateRepository : CoroutineCrudRepository<PollTemplateEntity, Long> {

    // 사용자용: 활성 상태(state=1) + 삭제 안 된 템플릿 조회
    @Query("""
        SELECT * FROM poll_templates
        WHERE state = 1 AND deleted_at IS NULL
        ORDER BY created_at DESC
    """)
    fun findAllActive(): Flow<PollTemplateEntity>

}