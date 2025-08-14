package kr.jiasoft.hiteen.feature.tbmq

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface MqttCredRepository : CoroutineCrudRepository<MqttCredEntity, Long> {
    suspend fun findByUserId(userId: Long): MqttCredEntity?
}
