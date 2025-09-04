package kr.jiasoft.hiteen.feature.soketi

import com.pusher.rest.Pusher
import com.pusher.rest.data.Result
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SoketiBroadcaster(
    @field:Value("\${soketi.host}") private val host: String,
    @field:Value("\${soketi.port}") private val port: Int,
    @field:Value("\${soketi.app-id}") private val appId: String,
    @field:Value("\${soketi.app-key}") private val appKey: String,
    @field:Value("\${soketi.app-secret}") private val appSecret: String,
) {
    private val pusher: Pusher = Pusher(
        "http://${appKey}:${appSecret}@${host}:${port}/apps/${appId}"
    )

    fun broadcast(channel: String, event: String, data: Map<String, Any>) {
        val result: Result = pusher.trigger(channel, event, data)
        if (result.status != Result.Status.SUCCESS) {
            throw RuntimeException("Soketi trigger failed: ${result.status} ${result.message}")
        }
    }

    fun broadcast(channels: List<String>, event: String, data: Map<String, Any>) {
        val result: Result = pusher.trigger(channels, event, data)
        if (result.status != Result.Status.SUCCESS) {
            throw RuntimeException("Soketi trigger failed: ${result.status} ${result.message}")
        }
    }
}