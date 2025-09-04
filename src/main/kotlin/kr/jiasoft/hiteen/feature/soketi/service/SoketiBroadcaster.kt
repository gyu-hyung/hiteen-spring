package kr.jiasoft.hiteen.feature.soketi.service

import com.pusher.rest.Pusher
import com.pusher.rest.data.Result
import kr.jiasoft.hiteen.feature.soketi.config.SoketiProperties
import org.springframework.stereotype.Component

@Component
class SoketiBroadcaster(
    props: SoketiProperties
) {
    private val pusher: Pusher

    init {
        val url =
            "http://${props.appKey}:${props.appSecret}@${props.host}:${props.port}/apps/${props.appId}"
        // ex : http://app-key:app-secret@host:port/apps/app-id
        this.pusher = Pusher(url)
    }

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
