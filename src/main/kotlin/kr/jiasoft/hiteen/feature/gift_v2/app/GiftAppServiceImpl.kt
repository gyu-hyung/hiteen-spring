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
import kr.jiasoft.hiteen.feature.giftishow.domain.GiftishowLogsEntity
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowLogsRepository
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
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
    private val giftishowLogsRepository: GiftishowLogsRepository,

): GiftAppService {


    @Value("\${giftishow.template-id}")
    private lateinit var templateId: String

    @Value("\${giftishow.banner-id}")
    private lateinit var bannerId: String

    @Value("\${giftishow.callback}")
    private lateinit var callbackNo: String


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
                if (giftUser.pubExpiredDate.isBefore(OffsetDateTime.now()))
                    throw IllegalArgumentException("발급만료일자가 지난 선물입니다.")

                val goodsEntity = giftUser.goodsCode?.let {
                    giftishowGoodsRepository.findByGoodsCode(it)
                        ?: throw IllegalArgumentException("상품 정보가 존재하지않습니다. 관리자에게 문의하세요.")
                } ?: throw IllegalArgumentException("상품 정보가 존재하지않습니다. 관리자에게 문의하세요.")

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
//                val trId = "4763632a21e04e91"

                // 🔹 1) 발송 요청
                val sendReq = GiftishowVoucherSendRequest(
                    goodsEntity.goodsCode,
                    "",
                    template.defaultMmsTitle,
                    mmsMsg,
                    req.phone ?: receiverUser!!.phone,
                    trId,
                    req.revInfoYn,
                    req.revInfoDate,
                    req.revInfoTime,
                    req.gubun,
                )

                // ▣ 1) 발행 요청
                val issued = giftishowClient.issueVoucher(sendReq)

                val d = issued.result?.result?.pinNo
                val dd = issued.result?.result?.couponImgUrl

                // ▣ 2) 상세 조회 (Map 기반)
                val res = giftishowClient.detailVoucher(trId)

                // result: List<Map<String, Any?>>
                val resultList = res["result"] as? List<Map<String, Any?>>
                val wrapper = resultList?.firstOrNull()                        // 첫 번째 wrapper

                val couponInfoList = wrapper?.get("couponInfoList") as? List<Map<String, Any?>>
                val detail = couponInfoList?.firstOrNull()                     // 첫 번째 쿠폰


                // ▣ 3) 유효기간 파싱
                val expireStr = detail?.get("validPrdEndDt") as? String
                require(!expireStr.isNullOrBlank()) { "Giftishow 응답에 validPrdEndDt 없음" }

                val expireAt = OffsetDateTime.parse(
                    expireStr + "+0900",
                    DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ")
                )



                // 🔹 3) GiftUser 업데이트
                giftUserRepository.save(
                    giftUser.copy(
                        status = GiftStatus.SENT.code,
                        requestDate = OffsetDateTime.now(),
                        couponNo = d,
                        couponImg = dd,
                        pubDate = OffsetDateTime.now(),
                        useExpiredDate = expireAt
                    )
                )

                // 🔹 4) 기프티쇼 로그 저장
                val log = GiftishowLogsEntity(
                    giftUserId = giftUser.id,
                    goodsCode = goodsEntity.goodsCode,
                    goodsName = goodsName,
                    orderNo = issued.result?.result?.orderNo,
                    mmsMsg = mmsMsg,
                    mmsTitle = template.defaultMmsTitle,
                    callbackNo = callbackNo,
                    phoneNo = req.phone ?: receiverUser!!.phone,
                    trId = trId,
                    reserveYn = req.revInfoYn,
                    reserveDate = req.revInfoDate,
                    reserveTime = req.revInfoTime,
                    templateId = templateId,
                    bannerId = bannerId,
                    userId = receiverUser!!.uid.toString(),
                    gubun = req.gubun,
                    response = issued.toString(),
                    code = issued.code,
                    message = issued.message,
                    pinNo = issued.result?.result?.pinNo,
                    couponImgUrl = issued.result?.result?.couponImgUrl,
                    memo = "",
                    status = GiftStatus.SENT.code,
                    createdAt = OffsetDateTime.now()
                )

                giftishowLogsRepository.save(log)
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

        // GiftRecord 리스트
        val records = giftUserRepository.findAllWithGiftUserByUserId(userId).toList()

        // goodsCode 리스트 (null 제거)
        val goodsCodes = records.mapNotNull { it.goodsCode }

        // goods 엔티티 조회
        val goodsList = giftishowGoodsRepository.findAllByGoodsCodeIn(goodsCodes).toList()

        // 빠르게 매핑하기 위한 Map<goodsCode, GoodsGiftishowEntity>
        val goodsMap: Map<String, GoodsGiftishowEntity> =
            goodsList.associateBy { it.goodsCode }

        // 각 record 에 해당하는 goods 를 넣어서 Response 생성
        return records.map { record ->
            val goods = record.goodsCode?.let { goodsMap[it] }
            record.toResponse(receiver, goods)
        }
    }



    override suspend fun listGoods() : List<GoodsGiftishowEntity> {
        return giftishowGoodsRepository.findAll().toList()
    }


}
