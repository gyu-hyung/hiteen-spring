package kr.jiasoft.hiteen.feature.gift.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("gift_users")
@Schema(description = "선물 > 받은 회원 목록 엔티티")
data class GiftUsersEntity(

    @Id
    @field:Schema(description = "고유 ID")
    val id: Long = 0,

    @field:Schema(description = "선물 번호 (FK: gift.id)")
    @Column("gift_id")
    val giftId: Long,

    @field:Schema(description = "받은 회원 ID (FK: users.id)")
    @Column("user_id")
    val userId: Long,

    @field:Schema(description = "수신 상태 (0: 대기, 1: 발송, 2: 사용, 3: 만료, 4: 배송요청, 5: 배송완료 등)")
    val status: Int = 0,

    @field:Schema(description = "받은 날짜")
    @Column("receive_date")
    val receiveDate: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "쿠폰 번호")
    @Column("coupon_no")
    val couponNo: String? = null,

    @field:Schema(description = "쿠폰 이미지 URL")
    @Column("coupon_img")
    val couponImg: String? = null,

    @field:Schema(description = "상품 발급(배송) 요청 일시")
    @Column("request_date")
    val requestDate: OffsetDateTime? = null,

    @field:Schema(description = "상품 발급(배송) 완료 일시")
    @Column("pub_date")
    val pubDate: OffsetDateTime? = null,

    @field:Schema(description = "상품 교환 사용 일시")
    @Column("use_date")
    val useDate: OffsetDateTime? = null,

    @field:Schema(description = "상품 발급 만료일")
    @Column("pub_expired_date")
    val pubExpiredDate: OffsetDateTime? = null,

    @field:Schema(description = "상품 사용 만료일")
    @Column("use_expired_date")
    val useExpiredDate: OffsetDateTime? = null,

    @field:Schema(description = "상품 코드")
    @Column("goods_code")
    val goodsCode: String? = null,

    @field:Schema(description = "게임 ID -- GiftCategory.Challenge 일 경우")
    @Column("season_id")
    val gameId: Long? = null,

    @field:Schema(description = "챌린지 회차번호 -- GiftCategory.Challenge 일 경우")
    @Column("season_id")
    val seasonId: Long? = null,

    @field:Schema(description = "챌린지 랭킹 -- GiftCategory.Challenge 일 경우")
    @Column("season_rank")
    val seasonRank: Int? = null,

    @field:Schema(description = "선물 받은 포인트 -- GiftType.Point 일 경우")
    @Column("point")
    val point: Int? = null,

    @field:Schema(description = "수령자 이름 -- GiftType.Delivery 일 경우")
    @Column("delivery_name")
    val deliveryName: String? = null,

    @field:Schema(description = "수령자 연락처 -- GiftType.Delivery 일 경우")
    @Column("delivery_phone")
    val deliveryPhone: String? = null,

    @field:Schema(description = "배송지 주소 -- GiftType.Delivery 일 경우")
    @Column("delivery_address1")
    val deliveryAddress1: String? = null,

    @field:Schema(description = "배송지 상세주소 -- GiftType.Delivery 일 경우")
    @Column("delivery_address2")
    val deliveryAddress2: String? = null,
)
