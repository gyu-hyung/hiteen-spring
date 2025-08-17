package kr.jiasoft.hiteen.feature.tbmq.subscription

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import kr.jiasoft.hiteen.feature.tbmq.TbmqAdminClient

@RestController
@RequestMapping("/api/mqtt")
class MqttSubscriptionController(
    private val service: MqttSubscriptionService
) {
    data class AddSubRequest(
        val clientId: String,
        val subscriptions: List<SubItem>
    ) {
        data class SubItem(
            val topicFilter: String,
            val qos: TbmqAdminClient.Qos = TbmqAdminClient.Qos.AT_MOST_ONCE,
            val options: Options? = null,
            val subscriptionId: Int? = null
        ) {
            data class Options(
                val noLocal: Boolean? = null,
                val retainAsPublish: Boolean? = null,
                val retainHandling: Int? = null // 0/1/2만 허용
            )
        }

        fun toCommand(): MqttSubscriptionService.AddSubsCommand =
            MqttSubscriptionService.AddSubsCommand(
                clientId = clientId,
                subscriptions = subscriptions.map {
                    TbmqAdminClient.Subscription(
                        topicFilter = it.topicFilter,
                        qos = it.qos,
                        options = it.options?.let { o ->
                            TbmqAdminClient.SubscriptionOptions(
                                noLocal = o.noLocal,
                                retainAsPublish = o.retainAsPublish,
                                retainHandling = o.retainHandling
                            )
                        },
                        subscriptionId = it.subscriptionId
                    )
                }
            )
    }

    @PostMapping("/subscription")
    suspend fun addSubscriptions(
        @Valid @RequestBody req: AddSubRequest
    ): ResponseEntity<Unit> {
        service.add(req.toCommand())
        return ResponseEntity.ok().build()
    }
}
