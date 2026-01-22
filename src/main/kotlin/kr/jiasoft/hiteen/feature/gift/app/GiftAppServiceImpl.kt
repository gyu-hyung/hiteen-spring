package kr.jiasoft.hiteen.feature.gift.app

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.toList
import kr.jiasoft.hiteen.feature.cash.app.CashService
import kr.jiasoft.hiteen.feature.cash.domain.CashPolicy
import kr.jiasoft.hiteen.feature.gift.domain.GiftCategory
import kr.jiasoft.hiteen.feature.gift.domain.GiftEntity
import kr.jiasoft.hiteen.feature.gift.domain.GiftMessageFormatter
import kr.jiasoft.hiteen.feature.gift.domain.GiftType
import kr.jiasoft.hiteen.feature.gift.domain.GiftUsersEntity
import kr.jiasoft.hiteen.feature.gift.domain.toTemplate
import kr.jiasoft.hiteen.feature.gift.dto.GiftBuyRequest
import kr.jiasoft.hiteen.feature.gift.infra.GiftRepository
import kr.jiasoft.hiteen.feature.gift.infra.GiftUserRepository
import kr.jiasoft.hiteen.feature.gift.dto.GiftProvideRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftIssueRequest
import kr.jiasoft.hiteen.feature.gift.dto.GiftResponse
import kr.jiasoft.hiteen.feature.gift.dto.GiftStatus
import kr.jiasoft.hiteen.feature.gift.dto.GiftUseRequest
import kr.jiasoft.hiteen.feature.gift.dto.client.GiftishowApiResponse
import kr.jiasoft.hiteen.feature.gift.dto.client.voucher.GiftishowVoucherSendRequest
import kr.jiasoft.hiteen.feature.gift.dto.toResponse
import kr.jiasoft.hiteen.feature.giftishow.domain.GiftishowLogsEntity
import kr.jiasoft.hiteen.feature.giftishow.domain.GoodsGiftishowEntity
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowGoodsRepository
import kr.jiasoft.hiteen.feature.giftishow.infra.GiftishowLogsRepository
import kr.jiasoft.hiteen.feature.play.infra.GameRepository
import kr.jiasoft.hiteen.feature.play.infra.SeasonRepository
import kr.jiasoft.hiteen.feature.point.app.PointService
import kr.jiasoft.hiteen.feature.point.domain.PointPolicy
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.app.event.PushSendRequestedEvent
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.user.app.UserService
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
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
    private val cashService: CashService,
    private val pushService: PushService,
    private val giftishowLogsRepository: GiftishowLogsRepository,

    private val txOperator: TransactionalOperator,

    private val eventPublisher: ApplicationEventPublisher,
    private val giftshowClient: GiftshowClient,
    private val objectMapper: ObjectMapper,
) : GiftAppService {


    @Value("\${giftishow.template-id}")
    private lateinit var templateId: String

    @Value("\${giftishow.banner-id}")
    private lateinit var bannerId: String

    @Value("\${giftishow.callback}")
    private lateinit var callbackNo: String


    override suspend fun findGift(receiverUserId: Long, giftUserId: Long) : GiftResponse {
        val userSummary = userService.findUserSummary(receiverUserId)
        val response = giftRepository.findWithGiftUserByUserId(receiverUserId, giftUserId)
            ?: throw IllegalArgumentException("No gift user found for $receiverUserId")
        val goods = response.goodsCode?.let {
            giftishowGoodsRepository.findByGoodsCode(it)
        }
        return response.toResponse(userSummary, goods)
    }


    /**
     * 상품 구매
     * 캐시 소진하여 기프티쇼 | 기프트 카드 구매
     */
    override suspend fun buyGift(
        userId: Long,
        userUid: UUID,
        req: GiftBuyRequest,
    ): List<GiftResponse> {
        return txOperator.executeAndAwait {

            val res = createGift(
                userId,
                GiftProvideRequest(
                    giftType = if (req.goodsCode.startsWith("G")) GiftType.Voucher else GiftType.GiftCard,
                    giftCategory = GiftCategory.Shop,
                    receiveUserUids = List(req.quantity) { req.receiveUserUid ?: userUid },
                    goodsCode = req.goodsCode,
                    memo = req.memo,
                ),
                sendPush = false
            )

            //캐시 차감
            val totalPrice = res.sumOf { it.goods?.realPrice ?: 0 }
            if (totalPrice > 0) {
                cashService.applyPolicy(userId, CashPolicy.BUY, res.first().giftUserId, -totalPrice)
            }

            // 캐시 차감 성공 후 푸시 알림 발송
            val uniqueUserIds = res.map { it.receiver.id }.distinct()
            val memo = res.firstOrNull()?.memo
            if (memo != null) {
                eventPublisher.publishEvent(
                    PushSendRequestedEvent(
                        userIds = uniqueUserIds,
                        actorUserId = null,
                        templateData = mapOf(
                            "code" to PushTemplate.GIFT_MESSAGE.code,
                            "title" to PushTemplate.GIFT_MESSAGE.title,
                            "message" to GiftCategory.Shop.toTemplate().defaultMemo!!
                        ),
                    )
                )
            }

            res
        }
    }


    /**
     * 선물을 지급합니다.(gift, giftUser 등록)
     * Type: Voucher, Delivery, GiftCard --Point, Cash 는 적립으로 처리
     * Category: Join, Challenge, Admin, Event, Shop
     * */
    override suspend fun createGift(userId: Long, req: GiftProvideRequest, sendPush: Boolean) : List<GiftResponse> {
        val receiverUsers = userRepository.findAllByUidIn(req.receiveUserUids)
        if (receiverUsers.isEmpty()) throw IllegalArgumentException("존재하지 않는 수신자")

        val memo = if (req.giftCategory == GiftCategory.Challenge) {
            GiftMessageFormatter.challengeMemo(
                gameName = req.gameId?.let { gameRepository.findById(it)?.name },
                seasonName = req.seasonId?.let {
                    val season = seasonRepository.findById(it)?: throw IllegalArgumentException("존재하지 않는 시즌")
                    "[" + season.month.toString() + "월 " + season.round.toString() + "회" + "]"
                },
                seasonRank = req.seasonRank
            )
        } else {
            req.memo ?: req.giftCategory.toTemplate().defaultMemo!!
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


        // 2️⃣ GiftUsers 생성 (여러명 또는 여러개 지원)
        // 수신자 UID 리스트(receiveUserUids)의 순서대로 각각 GiftUsers를 생성합니다.
        // (단일 유저에게 여러개를 보내는 경우 receiveUserUids에 동일 UID가 여러번 들어있을 수 있음)
        val receiverMap = receiverUsers.associateBy { it.uid.toString() }

        val giftUsers = req.receiveUserUids.mapNotNull { uid ->
            val user = receiverMap[uid.toString()] ?: return@mapNotNull null

            GiftUsersEntity(
                giftId = gift.id,
                userId = user.id,
                status = GiftStatus.WAIT.code,
                receiveDate = OffsetDateTime.now(),
                // GiftType Shop 이면 발급기한 무제한
                pubExpiredDate = if (req.giftCategory == GiftCategory.Shop) {
                    null
                } else {
                    OffsetDateTime.now().plusDays(30)
                },
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
        }

        val savedGiftUsers = giftUserRepository.saveAll(giftUsers).toList()

        // 푸시 알림 (선택적 발송)
        if (sendPush) {
            val uniqueUserIds = savedGiftUsers.map { it.userId }.distinct()
            eventPublisher.publishEvent(
                PushSendRequestedEvent(
                    userIds = uniqueUserIds,
                    actorUserId = null,
                    templateData = mapOf(
                        "code" to PushTemplate.GIFT_MESSAGE.code,
                        "title" to PushTemplate.GIFT_MESSAGE.title,
                        "message" to memo,
                    ),
                )
            )
        }

        return savedGiftUsers.map { findGift(it.userId, it.id) }
    }

    /**
     * 받은 giftUser 정보로 ( 기프티쇼 API 쿠폰발송 | 포인트 지급 | 배송요청 | 지급요청 )
     * pubExpiredDate 발급만료일자 지났는지?
     * type = Delivery 일때 주소 받았는지?
     * 발송 후 이력 저장
     * */
    override suspend fun issueGift(userId: Long, req: GiftIssueRequest) : GiftResponse {
        val gift = giftRepository.findByUid(req.giftUid)?:
            throw IllegalArgumentException("존재하지 않는 선물")
        val template = gift.category.toTemplate()
        val giftUser = giftUserRepository.findByGiftIdAndUserId(gift.id, userId)
        val receiverUser = userRepository.findById(giftUser.userId)

        // pubExpiredDate 발급만료일자 지났는지?
        if (giftUser.pubExpiredDate != null && giftUser.pubExpiredDate.isBefore(OffsetDateTime.now()))
            throw IllegalArgumentException("발급만료일자가 지난 선물입니다.")

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

            GiftType.Cash -> {
                cashService.applyPolicy(giftUser.userId, CashPolicy.ADMIN, gift.id, giftUser.point)
                giftUserRepository.save(giftUser.copy(
                    status = GiftStatus.USED.code,
                    requestDate = OffsetDateTime.now(),
                    pubDate = OffsetDateTime.now(),
                    useDate = OffsetDateTime.now(),
                ))
            }

            GiftType.Voucher -> {

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

                val pinNo = issued.result?.result?.pinNo
                val couponImgUrl = issued.result?.result?.couponImgUrl

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
                        couponNo = pinNo,
                        couponImg = couponImgUrl,
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


            GiftType.GiftCard -> {
                // GiftUser 상태 변경 (지급요청)
                giftUserRepository.save(giftUser.copy(
                    status = GiftStatus.GRANT_REQUESTED.code,
                    requestDate = OffsetDateTime.now(),
                ))
                // TODO 관리자에게 지급 요청 알림
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

    // TODO 선택 가능한 선물 목록 조회 관리자가 지정해놓은걸로(리그별)
    override suspend fun listGift(userId: Long): List<GiftResponse> {
        val receiver = userService.findUserSummary(userId)

        // GiftRecord 리스트
        val records = giftRepository.findAllWithGiftUserByUserId(userId).toList()

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


    /** 선물함 목록조회 (커서 기반) */
    override suspend fun listGiftByCursor(
        userId: Long,
        size: Int,
        lastId: Long?
    ): List<GiftResponse> {

        val receiver = userService.findUserSummary(userId)

        // 1️⃣ gift + gift_users 커서 조회
        val records = giftRepository
            .findAllWithGiftUserByUserIdCursor(
                userId = userId,
                lastId = lastId,
                size = size
            )
            .toList()

        if (records.isEmpty()) return emptyList()

        // 2️⃣ goodsCode 추출
        val goodsCodes = records.mapNotNull { it.goodsCode }.distinct()

        // 3️⃣ goods 조회
        val goodsMap = giftishowGoodsRepository
            .findAllByGoodsCodeIn(goodsCodes)
            .toList()
            .associateBy { it.goodsCode }

        // 4️⃣ Response 매핑
        return records.map { record ->
            val goods = record.goodsCode?.let { goodsMap[it] }
            record.toResponse(receiver, goods)
        }
    }


    override suspend fun listGoods() : List<GoodsGiftishowEntity> {
        return giftishowGoodsRepository.findAll().toList()
    }

    override suspend fun cancelVoucher(giftUid: UUID, giftUserId: Long): GiftishowApiResponse<String> {
        val giftUser = giftUserRepository.findById(giftUserId)
            ?: throw IllegalArgumentException("존재하지 않는 선물 수신 정보입니다.")

        val gift = giftRepository.findById(giftUser.giftId)
            ?: throw IllegalArgumentException("존재하지 않는 선물 정보입니다.")

        if (gift.uid != giftUid) {
            throw IllegalArgumentException("잘못된 선물 식별자입니다.")
        }

        val log = giftishowLogsRepository.findFirstByGiftUserIdOrderByCreatedAtDesc(giftUserId)
            ?: throw IllegalArgumentException("취소할 수 있는 발송 이력이 없습니다.")

        val trId = log.trId ?: throw IllegalArgumentException("trId를 찾을 수 없습니다.")

        // 1) 기본 취소 시도 (GET/POST 내부 구현)
        var response = giftshowClient.cancelVoucher(trId)

        if (response.code == "0000") {
            giftUserRepository.save(giftUser.copy(status = GiftStatus.CANCELLED.code))

            // update giftishow log
            try {
                val updatedLog = log.copy(
                    response = objectMapper.writeValueAsString(response),
                    code = response.code,
                    message = response.message,
                    status = GiftStatus.CANCELLED.code,
                    updatedAt = OffsetDateTime.now()
                )
                giftishowLogsRepository.save(updatedLog)
            } catch (ex: Exception) {
                println("[GiftAppService] failed to update giftishow log after cancel success: ${ex.message}")
            }

            return response
        }

        // 2) 취소 실패하면 재전송(retry) 한 번 시도
        println("[GiftAppService] cancel failed(code=${response.code}), trying cancel again")
        try {
            val secondResp = giftshowClient.cancelVoucher(trId)
            println("[GiftAppService] second cancel response: code=${secondResp.code}, message=${secondResp.message}")
            if (secondResp.code == "0000") {
                giftUserRepository.save(giftUser.copy(status = GiftStatus.CANCELLED.code))

                // update giftishow log with second response
                try {
                    val updatedLog = log.copy(
                        response = objectMapper.writeValueAsString(secondResp),
                        code = secondResp.code,
                        message = secondResp.message,
                        status = GiftStatus.CANCELLED.code,
                        updatedAt = OffsetDateTime.now()
                    )
                    giftishowLogsRepository.save(updatedLog)
                } catch (ex: Exception) {
                    println("[GiftAppService] failed to update giftishow log after second cancel success: ${ex.message}")
                }

                return secondResp
            }
            // continue to detail check if second attempt not successful
        } catch (ex: Exception) {
            println("[GiftAppService] second cancel attempt exception: ${ex.message}")
        }

        // 3) 재전송도 실패하면 상세조회해서 상태 확인 (폐기 상태면 완료 처리)
        println("[GiftAppService] retry failed or non-success, checking voucher detail for trId=$trId")
        val detail = try {
            giftshowClient.detailVoucher(trId)
        } catch (ex: Exception) {
            println("[GiftAppService] detailVoucher failed: ${ex.message}")
            null
        }

        // detail 구조: { "result": [ { "couponInfoList": [ { "pinStatusCd": "07", "pinStatusNm": "구매취소(폐기)", ... } ], "resCode": "0000" } ] }
        val isDisposed = try {
            val resultList = (detail?.get("result") as? List<Map<String, Any?>>)
            val wrapper = resultList?.firstOrNull()
            val couponInfoList = wrapper?.get("couponInfoList") as? List<Map<String, Any?>>
            val first = couponInfoList?.firstOrNull()
            val pinStatusCd = first?.get("pinStatusCd") as? String
            val pinStatusNm = first?.get("pinStatusNm") as? String

            (pinStatusCd == "07") || (pinStatusNm?.contains("폐기") == true)
        } catch (ex: Exception) {
            println("[GiftAppService] parse detail failed: ${ex.message}")
            false
        }

        if (isDisposed) {
            giftUserRepository.save(giftUser.copy(status = GiftStatus.CANCELLED.code))
            // update log with detail info
            try {
                val detailJson = detail?.let { objectMapper.writeValueAsString(it) }
                val updatedLog = log.copy(
                    response = detailJson ?: log.response,
                    code = "0000",
                    message = "disposed",
                    status = GiftStatus.CANCELLED.code,
                    updatedAt = OffsetDateTime.now()
                )
                giftishowLogsRepository.save(updatedLog)
            } catch (ex: Exception) {
                println("[GiftAppService] failed to update giftishow log after detail disposed: ${ex.message}")
            }

            return GiftishowApiResponse(code = "0000", message = "disposed", result = "disposed")
        }

        // 모두 실패한 경우 원래 응답을 그대로 전달하거나 예외로 처리
        throw IllegalArgumentException("기프티쇼 취소/재전송/상세조회로도 폐기 확인되지 않음: ${response.message}")
    }

    override suspend fun deleteGift(giftUid: UUID, giftUserId: Long): Any? {
        val giftUser = giftUserRepository.findById(giftUserId)
            ?: throw IllegalArgumentException("존재하지 않는 선물 수신 정보입니다.")

        val gift = giftRepository.findById(giftUser.giftId)
            ?: throw IllegalArgumentException("존재하지 않는 선물 정보입니다.")

        if (gift.uid != giftUid) throw IllegalArgumentException("잘못된 선물 식별자입니다.")

        return when (gift.type) {
            GiftType.Voucher -> {
                // try cancel via giftshow
                val resp = cancelVoucher(giftUid, giftUserId)
                if (resp.code == "0000") {
                    mapOf("result" to "cancelled")
                } else {
//                    mapOf("result" to "failed", "reason" to resp.message)
                    throw IllegalArgumentException("기프티쇼 취소 실패: ${resp.message}")
                }
            }
            else -> {
                // non-voucher: mark cancelled locally
                giftUserRepository.save(giftUser.copy(status = GiftStatus.CANCELLED.code))
                mapOf("result" to "marked_cancelled")
            }
        }
    }


}
