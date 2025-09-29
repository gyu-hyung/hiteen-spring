package kr.jiasoft.hiteen.feature.push.infra

import kr.jiasoft.hiteen.feature.push.domain.PushDetailEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PushDetailRepository : CoroutineCrudRepository<PushDetailEntity, Long> {

    @Query("SELECT * FROM push_detail WHERE push_id = :pushId")
    suspend fun findAllByPushId(pushId: Long): List<PushDetailEntity>
}