package kr.jiasoft.hiteen.feature

import com.pusher.rest.Pusher
import org.junit.jupiter.api.Test

class SoketiConnectionTest {

    @Test
    fun sendTestEvent() {
        val pusher = Pusher("49.247.170.115", "hiteen-key", "hiteen-secret")
        pusher.setEncrypted(false)
        pusher.trigger("test-channel", "test-event", mapOf("message" to "Hello Soketi!"))
        println("✅ Soketi test event sent successfully.")
    }
}
