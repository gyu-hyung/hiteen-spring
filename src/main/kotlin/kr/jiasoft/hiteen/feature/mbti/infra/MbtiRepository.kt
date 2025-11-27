package kr.jiasoft.hiteen.feature.mbti.infra

import kr.jiasoft.hiteen.feature.mbti.domain.MbtiResultEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface MbtiRepository : CoroutineCrudRepository<MbtiResultEntity, Long> {

    suspend fun findByUserId(userId: Long): MbtiResultEntity?

}
