package kr.jiasoft.hiteen.feature.integration.tbmq.credential.infra

import kr.jiasoft.hiteen.feature.integration.tbmq.credential.domain.MqttCredEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface MqttCredRepository : CoroutineCrudRepository<MqttCredEntity, Long> {
    suspend fun findByUserId(userId: Long): MqttCredEntity?
    suspend fun findByClientId(credentialsId: String): MqttCredEntity?
}
