package kr.jiasoft.hiteen.feature.gift.app

import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import kr.jiasoft.hiteen.feature.gift.domain.GiftMessageFormatter
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.domain.GiftUsersEntity
import kr.jiasoft.hiteen.feature.gift.domain.toTemplate
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.giftishow.app.GiftishowSendService
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.buildPushData
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.OffsetDateTime

@Service
class GiftSendOrchestratorService(
    private val giftRepository: GiftRepository,
    private val giftUsersRepository: GiftUserRepository,
    private val giftishowGoodsRepository: GiftishowGoodsRepository,
    private val seasonRepository: SeasonRepository,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,

    private val giftishowSendService: GiftishowSendService,
    private val pointService: PointService,
    private val pushService: PushService,
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
        gameId: Long? = null,
        seasonId: Long? = null,
        seasonRank: Int? = null,
        point: Int? = null,
        deliveryName: String? = null,
        deliveryPhone: String? = null,
        deliveryAddress1: String? = null,
        deliveryAddress2: String? = null,
    ): GiftUsersEntity {

        val receiverUser = userRepository.findById(receiverUserId) ?: throw IllegalArgumentException("존재하지 않는 수신자")

        val template = giftCategory.toTemplate()

        // 기프티쇼 상품명 조회 (Voucher일 수 있으므로 미리 조회)
        val goodsEntity = goodsCode?.let { giftishowGoodsRepository.findByGoodsCode(it) }

        val memo = if (giftCategory == GiftCategory.Challenge) {
            GiftMessageFormatter.challengeMemo(
                gameName = gameId?.let { gameRepository.findById(it)?.name },
                seasonName = seasonId?.let {
                    val season = seasonRepository.findById(it)?: throw kotlin.IllegalArgumentException("존재하지 않는 시즌")
                    "[" + season.month.toString() + "월 " + season.round.toString() + "회" + "]"
                },
                seasonRank = seasonRank
            )
        } else {
            template.defaultMemo!!
        }

        val goodsName = if (giftCategory == GiftCategory.Challenge) {
            GiftMessageFormatter.challengeGoodsName(goodsEntity?.goodsName)
        } else {
            template.defaultGoodsName!!
        }

        val mmsMsg = if (giftCategory == GiftCategory.Challenge) {
            GiftMessageFormatter.challengeMmsMsg(goodsName)
        } else {
            template.defaultMmsMsg!!
        }


        // 1️⃣ Gift 생성
        val gift = giftRepository.save(
            GiftEntity(
                type = giftType,
                category = giftCategory,
                userId = senderUserId,
                memo = memo,
            )
        )

        // 2️⃣ GiftUsers 생성
        var giftUser = giftUsersRepository.save(
            GiftUsersEntity(
                giftId = gift.id,
                userId = receiverUserId,
                status = 0,
                requestDate = OffsetDateTime.now(),
                goodsCode = goodsCode,
                gameId = gameId,
                seasonId = seasonId,
                seasonRank = seasonRank,
                point = point,
                deliveryName = deliveryName,
                deliveryPhone = deliveryPhone,
                deliveryAddress1 = deliveryAddress1,
                deliveryAddress2 = deliveryAddress2,
            )
        )

        // 3️⃣ VOUCHER 처리 (기프티쇼 API 호출)
        if (giftType == GiftType.Voucher && goodsCode != null) {

            val giftishowLog = giftishowSendService.sendCoupon(
                giftUserId = giftUser.id,
                goodsCode = goodsCode,
                phone = receiverUser.username,
                goodsName = goodsName,
                mmsTitle = template.defaultMmsTitle,
                mmsMsg = mmsMsg,
            )

            giftUser = giftUsersRepository.save(
                giftUser.copy(
                    status = if (giftishowLog.code == "0000") 1 else -1,
                    pubDate = OffsetDateTime.now(),
                    couponNo = giftishowLog.pinNo,
                    couponImg = giftishowLog.couponImgUrl
                )
            )
        }

        // 4️⃣ POINT 지급 처리
        if (giftType == GiftType.Point) {
            pointService.applyPolicy(receiverUserId, PointPolicy.ADMIN, gift.id, point)
        }

        // 메세지 발송
        pushService.sendAndSavePush(listOf(receiverUser.id), mapOf(
            "code" to PushTemplate.GIFT_MESSAGE.code,
            "title" to PushTemplate.GIFT_MESSAGE.title,
            "message" to memo
        ))

        return giftUser
    }

}
