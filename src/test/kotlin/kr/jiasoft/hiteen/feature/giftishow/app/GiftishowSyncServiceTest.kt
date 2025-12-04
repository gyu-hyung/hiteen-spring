package kr.jiasoft.hiteen.feature.giftishow.app

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.gift_v2.app.GiftshowClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.Test

@SpringBootTest
class GiftishowSyncServiceTest {

    @Autowired
    lateinit var giftishowSyncService: GiftishowSyncService

    @Autowired
    lateinit var giftishowClient: GiftshowClient

    @Test
    fun `기프티쇼 상품 정보 import` (){
        runBlocking {
            giftishowSyncService.syncGoods()
            giftishowSyncService.syncBrandsAndCategories()
        }
    }


    @Test
    fun `비즈머니` (){
        runBlocking {
            val res = giftishowClient.bizMoney()
            println("res = ${res}")
        }
    }


}