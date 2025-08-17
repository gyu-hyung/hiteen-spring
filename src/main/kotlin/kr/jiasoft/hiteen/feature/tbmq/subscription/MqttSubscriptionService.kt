package kr.jiasoft.hiteen.feature.tbmq.subscription

import kr.jiasoft.hiteen.feature.tbmq.TbmqAdminClient
import kr.jiasoft.hiteen.feature.tbmq.credential.MqttCredRepository
import org.springframework.stereotype.Service

@Service
class MqttSubscriptionService(
    private val tbmq: TbmqAdminClient,
    private val credRepository: MqttCredRepository // 유저-클라이언트 매핑 확인 용(있다면)
) {
    data class AddSubsCommand(
        val clientId: String,
        val subscriptions: List<TbmqAdminClient.Subscription>
    )

    suspend fun add(command: AddSubsCommand) {
//         (선택) clientId 검증: 우리 DB에 등록된 clientId인지 확인
         credRepository.findByClientIdOrThrow(command.clientId)

        // (선택) 토픽 유효성/개수 제한
        // validateTopics(command.subscriptions.map { it.topicFilter })

        tbmq.addSubscriptions(command.clientId, command.subscriptions)
    }
}
