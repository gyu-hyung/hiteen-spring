package kr.jiasoft.hiteen.feature.gift_v2.app

import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import kr.jiasoft.hiteen.feature.gift.domain.GiftMessageFormatter
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.domain.GiftUsersEntity
import kr.jiasoft.hiteen.feature.gift.domain.toTemplate
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftCreateRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftIssueRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftStatus
import kr.jiasoft.hiteen.feature.gift_v2.dto.GiftUseRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.client.voucher.GiftishowVoucherSendRequest
import kr.jiasoft.hiteen.feature.gift_v2.dto.toResponse
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class GiftAppServiceImpl (
    private val giftishowClient: GiftshowClient,

    private val userService: UserService,

    private val userRepository: UserRepository,
    private val gameRepository: GameRepository,
    private val seasonRepository: SeasonRepository,
    private val giftishowGoodsRepository: GiftishowGoodsRepository,

    private val giftRepository: GiftRepository,
    private val giftUserRepository: GiftUserRepository,

    private val pointService: PointService,
    private val pushService: PushService,

): GiftAppService {


    private suspend fun findGift(receiverUserId: Long, giftUserId: Long) : GiftResponse{
        val userSummary = userService.findUserSummary(receiverUserId)
        val response = giftUserRepository.findWithGiftUserByUserId(receiverUserId, giftUserId)
        val goods = response.goodsCode?.let {
            giftishowGoodsRepository.findByGoodsCode(it)
        }
        return response.toResponse(userSummary, goods)
    }


    /**
     * 관리자가 사용자에게 선물을 지급합니다.(gift, giftUser 등록)
     * Type: Point, Voucher, Delivery, Etc
     * Category: Join, Challenge, Admin, Event
     * */
    override suspend fun createGift(userId: Long, req: GiftCreateRequest) : GiftResponse {
        val receiverUser = userRepository.findByUid(req.receiveUserUid.toString())
            ?: throw IllegalArgumentException("존재하지 않는 수신자")


        val memo = if (req.giftCategory == GiftCategory.Challenge) {
            GiftMessageFormatter.challengeMemo(
                gameName = req.gameId?.let { gameRepository.findById(it)?.name },
                seasonName = req.seasonId?.let {
                    val season = seasonRepository.findById(it)?: throw kotlin.IllegalArgumentException("존재하지 않는 시즌")
                    "[" + season.month.toString() + "월 " + season.round.toString() + "회" + "]"
                },
                seasonRank = req.seasonRank
            )
        } else {
            req.giftCategory.toTemplate().defaultMemo!!
        }


        // 1️⃣ Gift 생성
        val gift = giftRepository.save(
            GiftEntity(
                type = req.giftType,
                category = req.giftCategory,
                userId = userId,
                memo = memo,
            )
        )


        // 2️⃣ GiftUsers 생성
        val giftUser = giftUserRepository.save(
            GiftUsersEntity(
                giftId = gift.id,
                userId = receiverUser.id,
                status = GiftStatus.WAIT.code,
                receiveDate = OffsetDateTime.now(),
                pubExpiredDate = OffsetDateTime.now().plusMonths(1),// 한달 안에 발급받아야함
                goodsCode = req.goodsCode,
                gameId = req.gameId,
                seasonId = req.seasonId,
                seasonRank = req.seasonRank,
                point = req.point,
                deliveryName = req.deliveryName,
                deliveryPhone = req.deliveryPhone,
                deliveryAddress1 = req.deliveryAddress1,
                deliveryAddress2 = req.deliveryAddress2,
            )
        )

        // 푸시 전송
        pushService.sendAndSavePush(listOf(receiverUser.id), mapOf(
            "code" to PushTemplate.GIFT_MESSAGE.code,
            "title" to PushTemplate.GIFT_MESSAGE.title,
            "message" to memo
        ))

        return findGift(receiverUser.id, giftUser.id)
    }

    /**
     * 받은 giftUser 정보로 ( 기프티쇼 API 쿠폰발송 | 포인트 지급 | 배송요청 )
     * pubExpiredDate 발급만료일자 이전인가?
     * type = Delivery 일때 주소 받았는지?
     * 발송 후 이력 저장
     * */
    override suspend fun issueGift(userId: Long, req: GiftIssueRequest) : GiftResponse {
        val gift = giftRepository.findByUid(req.giftUid)?:
            throw IllegalArgumentException("존재하지 않는 선물")
        val template = gift.category.toTemplate()
        val giftUser = giftUserRepository.findByGiftIdAndUserId(gift.id, userId)
        val receiverUser = userRepository.findById(giftUser.userId)

        when (gift.type) {

            GiftType.Point -> {
                pointService.applyPolicy(giftUser.userId, PointPolicy.ADMIN, gift.id, giftUser.point)
                giftUserRepository.save(giftUser.copy(
                    status = GiftStatus.USED.code,
                    requestDate = OffsetDateTime.now(),
                    pubDate = OffsetDateTime.now(),
                    useDate = OffsetDateTime.now(),
                ))
            }

            GiftType.Voucher -> {
                // pubExpiredDate 발급만료일자 이전인가?
                if(giftUser.pubExpiredDate.isBefore(OffsetDateTime.now()))
                    throw IllegalArgumentException("발급만료일자가 지난 선물입니다.")

                val goodsEntity = giftUser.goodsCode?.let {
                    giftishowGoodsRepository.findByGoodsCode(it)?:
                    throw IllegalArgumentException("상품 정보가 존재하지않습니다. 관리자에게 문의하세요.")
                }?: throw IllegalArgumentException("상품 정보가 존재하지않습니다. 관리자에게 문의하세요")

                val goodsName = if (gift.category == GiftCategory.Challenge) {
                    GiftMessageFormatter.challengeGoodsName(goodsEntity.goodsName)
                } else {
                    template.defaultGoodsName!!
                }

                val mmsMsg = if (gift.category == GiftCategory.Challenge) {
                    GiftMessageFormatter.challengeMmsMsg(goodsName)
                } else {
                    template.defaultMmsMsg!!
                }


                val trId = UUID.randomUUID().toString().replace("-", "").take(16)

                val coupon = giftishowClient.issueVoucher(GiftishowVoucherSendRequest(
                    goodsEntity.goodsCode,
                    "",
                    template.defaultMmsTitle,
                    mmsMsg,
                    req.phone?: receiverUser!!.phone,
                    trId,
                    req.revInfoYn,
                    req.revInfoDate,
                    req.revInfoTime,
                    req.gubun,
                ))

                val couponDetail = giftishowClient.detailVoucher(trId)

                giftUserRepository.save(giftUser.copy(
                    status = GiftStatus.SENT.code,
                    requestDate = OffsetDateTime.now(),
                    couponNo = coupon.result?.pinNo,
                    couponImg = coupon.result?.couponImgUrl,
                    pubDate = OffsetDateTime.now(),
                    useExpiredDate = OffsetDateTime.parse(couponDetail.result?.validPrdEndDt!!, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                ))
            }

            GiftType.Delivery -> {
                giftUserRepository.save(giftUser.copy(
//                    status = 4,//`배송요청` 상태 TODO 배송완료 시 어캐 상태변경함? 배치?
                    status = GiftStatus.DELIVERY_REQUESTED.code,
                    requestDate = OffsetDateTime.now(),
                    deliveryName = req.deliveryName,
                    deliveryPhone = req.deliveryPhone,
                    deliveryAddress1 = req.deliveryAddress1,
                    deliveryAddress2 = req.deliveryAddress2,
                ))
                //TODO 푸시? 누구에게?
            }
        }

        return findGift(userId, giftUser.id)
    }

    // 사용 완료 처리
    override suspend fun useGift(userId: Long, req: GiftUseRequest) : GiftResponse {
        val gift = giftRepository.findByUid(req.giftUid)
            ?: throw IllegalArgumentException("존재하지 않는 정보")
        val giftUser = giftUserRepository.findByGiftIdAndUserId(gift.id, userId)
        giftUserRepository.save(giftUser.copy(
            status = GiftStatus.USED.code,
            useDate = OffsetDateTime.now(),
        ))
        return findGift(userId, giftUser.id)
    }

    override suspend fun listGift(userId: Long): List<GiftResponse> {
        val receiver = userService.findUserSummary(userId)

        val records = giftUserRepository.findAllWithGiftUserByUserId(userId).toList()

        return records.map { it.toResponse(receiver) }
    }

    override suspend fun listGoods() : List<GoodsGiftishowEntity> {
        return giftishowGoodsRepository.findAll().toList()
    }


}
