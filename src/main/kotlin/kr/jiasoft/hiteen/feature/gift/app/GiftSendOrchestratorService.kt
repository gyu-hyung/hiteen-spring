package kr.jiasoft.hiteen.feature.gift.app

import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.domain.GiftUsersEntity
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.giftishow.app.GiftishowSendService
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class GiftSendOrchestratorService(
    private val giftRepository: GiftRepository,
    private val giftUsersRepository: GiftUserRepository,
    private val giftishowGoodsRepository: GiftishowGoodsRepository,
    private val userRepository: UserRepository,

    private val giftishowSendService: GiftishowSendService,
    private val pointService: PointService,
) {

    /**
     * @param senderUserId  선물 보낸 사용자
     * @param receiverUserId 받는 사용자
     * @param goodsCode 기프티쇼 상품 코드
     * @param phone 수신 번호
     */
    suspend fun sendGift(
        giftType: GiftType,
        giftCategory: GiftCategory,
        senderUserId: Long,
        receiverUserId: Long,
        goodsCode: String? = null,
        seasonId: Long? = null,
        seasonRank: Int? = null,
        point: Int? = null,
        deliveryName: String? = null,
        deliveryPhone: String? = null,
        deliveryAddress1: String? = null,
        deliveryAddress2: String? = null,
    ): GiftUsersEntity {

        val receiverUser = userRepository.findById(receiverUserId)
        goodsCode?.let {
            giftishowGoodsRepository.findByGoodsCode(it)
                ?: throw IllegalArgumentException("상품 코드 ${it} (이)가 존재하지 않습니다.")
        }
        assert(receiverUser != null)

        // 1️⃣ Gift 테이블 생성 (선물 묶음)
        val gift = giftRepository.save(
            GiftEntity(
                type = giftType,
                category = giftCategory,
                userId = senderUserId,
                memo = "랭킹 보상",
            )
        )

        // 2️⃣ GiftUsers 생성 (수신자)
        var giftUser = giftUsersRepository.save(
            GiftUsersEntity(
                giftId = gift.id,
                userId = receiverUserId,
                status = 0, // 0 = 준비
                requestDate = OffsetDateTime.now(),
                goodsCode = goodsCode,
                seasonId = seasonId,
                seasonRank = seasonRank,
                point = point,
                deliveryName = deliveryName,
                deliveryPhone = deliveryPhone,
                deliveryAddress1 = deliveryAddress1,
                deliveryAddress2 = deliveryAddress2,
            )
        )

        if(GiftType.Voucher == giftType) {
            assert(goodsCode != null)
            // 3️⃣ 기프티쇼 API 호출 giftishowLog 는 sendCoupon 안에서 쌓임.
            val giftishowLog = giftishowSendService.sendCoupon(
                giftUserId = giftUser.id,
                goodsCode = goodsCode!!,
                phone = receiverUser?.username.toString(),
                goodsName = "하이틴 랭킹 리워드",
                mmsTitle = "[하이틴] 기프티콘 도착!",
                mmsMsg = "쿠폰이 도착했어요 ~ 🎁",
            )

            // 4️⃣ API 응답 기반 GiftUsers Update
            giftUser = giftUsersRepository.save(
                giftUser.copy(
                    status = if (giftishowLog.code == "0000") 1 else -1, // 성공 1, 실패 -1
                    pubDate = OffsetDateTime.now(),
                    couponNo = giftishowLog.pinNo,
                    couponImg = giftishowLog.couponImgUrl
                )
            )
        } else if (GiftType.Point == giftType){
            pointService.applyPolicy(
                receiverUserId, PointPolicy.ADMIN, gift.id, point
            )
        }

        return giftUser
    }
}
