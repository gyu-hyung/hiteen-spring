package kr.jiasoft.hiteen.feature.push.app

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class PushServiceTest {

    @Autowired
    private lateinit var pushService: PushService


//    @Test
//    fun testSendAndSavePush() {
//        runBlocking{
//            pushService.sendAndSavePush(
//                listOf(1),
//                PushTemplate.FRIEND_REQUEST.buildPushData("nickname" to "날씬한대지")
//                PushTemplate.FRIEND_ACCEPT.buildPushData("nickname" to "홍길동")
//                        PushTemplate.FRIEND_REQUEST.buildPushData("nickname" to "날씬한대지")
//                        PushTemplate.FRIEND_REQUEST.buildPushData("nickname" to "날씬한대지")
//                        PushTemplate.FRIEND_REQUEST.buildPushData("nickname" to "날씬한대지")
//                        PushTemplate.FRIEND_REQUEST.buildPushData("nickname" to "날씬한대지")
//            )
//        }
//    }

}