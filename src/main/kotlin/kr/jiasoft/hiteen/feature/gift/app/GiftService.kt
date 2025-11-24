package kr.jiasoft.hiteen.feature.gift.app

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.gift.dto.GiftRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftUserResponse
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.gift.mapper.GiftMapper
import kr.jiasoft.hiteen.feature.giftishow.app.GiftishowService
import kr.jiasoft.hiteen.feature.user.app.UserService
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class GiftService(
    private val giftRepository: GiftRepository,
    private val giftUserRepository: GiftUserRepository,
    private val giftishowService: GiftishowService,
    private val userService: UserService,
    private val mapper: GiftMapper
) {

//    suspend fun createGift(request: GiftRequest): GiftResponse {
//        val entity = mapper.toEntity(request)
//        val saved = giftRepository.save(entity)
//        return mapper.toResponse(saved)
//    }
//
//    suspend fun getGifts(): List<GiftResponse> =
//        giftRepository.findAll().map(mapper::toResponse).toList()


    suspend fun getMyGifts(userId: Long): List<GiftUserResponse> {
        val giftUsers = giftUserRepository.findByUserIdOrderByIdDesc(userId)

        return giftUsers.map { giftUser ->


            GiftUserResponse(
                status = when (giftUser.status) {
                    0 -> "WAITING"
                    1 -> "SENT"
                    2 -> "USED"
                    3 -> "EXPIRED"
                    else -> "UNKNOWN"
                },
                issuedAt = giftUser.receiveDate ?: giftUser.requestDate ?: OffsetDateTime.now(),
                expireAt = giftUser.useExpiredDate?.atStartOfDay()?.atOffset(OffsetDateTime.now().offset),
                user = userService.findUserSummary(giftUser.userId),
                goods = giftUser.goodsCode?.let { giftishowService.getGoods(giftUser.goodsCode) }
            )
        }
    }






}