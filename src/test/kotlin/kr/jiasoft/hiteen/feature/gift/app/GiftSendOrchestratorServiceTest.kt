package kr.jiasoft.hiteen.feature.gift.app

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class GiftSendOrchestratorServiceTest {

    @Autowired
    lateinit var service: GiftSendOrchestratorService

    @Test
    fun voucherGiftTest() {
        runBlocking {
            service.sendGift(
                giftType = GiftType.Voucher,
                giftCategory = GiftCategory.Challenge,
                senderUserId = 1,
                receiverUserId = 3,
                goodsCode = "G00000280811",
                seasonId = 4,
                seasonRank = 1,
            )
        }
    }


    @Test
    fun pointGiftTest() {
        runBlocking {
            service.sendGift(
                giftType = GiftType.Point,
                giftCategory = GiftCategory.Admin,
                senderUserId = 1,
                receiverUserId = 3,
                point = 1000,

            )
        }
    }


    @Test
    fun deliveryGiftTest() {
        runBlocking {
            service.sendGift(
                giftType = GiftType.Delivery,
                giftCategory = GiftCategory.Admin,
                senderUserId = 1,
                receiverUserId = 3,
                deliveryName = "홍길동",
                deliveryPhone = "01095393637",
                deliveryAddress1 = "서울특별시 강남구 봉은사로 1",
                deliveryAddress2 = "2층",
            )
        }
    }


}

