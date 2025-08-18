package kr.jiasoft.hiteen.feature.tbmq.credential

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface MqttCredRepository : CoroutineCrudRepository<MqttCredEntity, Long> {
    suspend fun findByUserId(userId: Long): MqttCredEntity?
    suspend fun findByClientId(credentialsId: String): MqttCredEntity?
}
