package kr.jiasoft.hiteen.feature.gift_v2.app

import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import org.springframework.stereotype.Service

@Service
class GiftService2 (
    private val giftRepository: GiftRepository,

) {

    //Point, Voucher, Delivery, Etc
    //Join, Challenge, Admin, Event
//    suspend fun createGift(req: GiftCreateRequest): GiftEntity {
//        return when (req.giftCategory) {
//            GiftCategory.Admin -> {
//                giftRepository.save(req.toGiftEntity())
//            }
//            GiftCategory.Challenge -> {
//                giftRepository.save(req.toGiftEntity())
//            }
//            GiftCategory.Event -> {
//                giftRepository.save(req.toGiftEntity())
//            }
//            GiftCategory.Join -> {
//                giftRepository.save(req.toGiftEntity())
//            }
//        }
//    }



}