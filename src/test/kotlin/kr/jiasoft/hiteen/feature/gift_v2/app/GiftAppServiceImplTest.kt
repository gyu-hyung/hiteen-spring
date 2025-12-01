package kr.jiasoft.hiteen.feature.gift_v2.app

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftCreateRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

@SpringBootTest
class GiftAppServiceImplTest {

    @Autowired
    private lateinit var giftAppService: GiftAppService

    @Autowired
    private lateinit var giftUserRepository: GiftUserRepository

    @Test
    fun `find test`() = runTest {
        val res = giftUserRepository.findAllWithGiftUserByUserId(1).toList()
        println(res)
    }


    @Test
    fun `선물 주기`(){
        runBlocking {
            //포인트 선물
            giftAppService.createGift(
                1,
                GiftCreateRequest(
                    giftType = GiftType.Point,
                    giftCategory = GiftCategory.Admin,
                    receiveUserUid = UUID.fromString("6e330bdc-3062-4a14-80f2-a46e04278c5c"),
                    point = 400,
                )
            )

            //기프티쇼 선물
//            giftAppService.createGift(
//                1,
//                GiftCreateRequest(
//                    giftType = GiftType.Voucher,
//                    giftCategory = GiftCategory.Challenge,
//                    receiveUserUid = UUID.fromString("6e330bdc-3062-4a14-80f2-a46e04278c5c"),
//                    goodsCode = "G00000280811",
//                    gameId = 1,
//                    seasonId = 7,
//                    seasonRank = 1,
//                )
//            )
//
//            //배송 선물
//            giftAppService.createGift(
//                1,
//                GiftCreateRequest(
//                    giftType = GiftType.Point,
//                    giftCategory = GiftCategory.Join,
//                    receiveUserUid = UUID.randomUUID(),
//                    memo = "test",
//                    goodsCode = "test",
//                    gameId = 1,
//                    seasonId = 1,
//                    seasonRank = 1,
//                    point = 1,
//                    deliveryName = "test",
//                    deliveryPhone = "test",
//                    deliveryAddress1 = "test",
//                    deliveryAddress2 = "test"
//                )
//            )
        }
    }

}