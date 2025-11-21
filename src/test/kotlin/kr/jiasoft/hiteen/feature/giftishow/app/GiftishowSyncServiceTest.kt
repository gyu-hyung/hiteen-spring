package kr.jiasoft.hiteen.feature.giftishow.app

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class GiftishowSyncServiceTest {

    @Autowired
    lateinit var giftishowSyncService: GiftishowSyncService

    @Test
    fun `기프티쇼 상품 정보 import` (){
        runBlocking {
            giftishowSyncService.syncGoods()
        }
    }

}