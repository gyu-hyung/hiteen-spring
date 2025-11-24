package kr.jiasoft.hiteen.feature.giftishow.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("giftishow_logs")
@Schema(description = "기프티쇼 발송 로그 엔티티")
data class GiftishowLogsEntity(

    @Id
    @field:Schema(description = "고유 ID")
    val id: Long = 0,

    @field:Schema(description = "선물 사용자 ID (FK: gift_users.id)")
    @Column("gift_user_id")
    val giftUserId: Long? = null,

    @field:Schema(description = "상품 코드")
    @Column("goods_code")
    val goodsCode: String? = null,

    @field:Schema(description = "상품명")
    @Column("goods_name")
    val goodsName: String? = null,

    @field:Schema(description = "주문번호")
    @Column("order_no")
    val orderNo: String? = null,

    @field:Schema(description = "MMS 메시지")
    @Column("mms_msg")
    val mmsMsg: String? = null,

    @field:Schema(description = "MMS 제목")
    @Column("mms_title")
    val mmsTitle: String? = null,

    @field:Schema(description = "발신번호")
    @Column("callback_no")
    val callbackNo: String? = null,

    @field:Schema(description = "수신번호")
    @Column("phone_no")
    val phoneNo: String? = null,

    @field:Schema(description = "거래 ID")
    @Column("tr_id")
    val trId: String? = null,

    @field:Schema(description = "예약 여부 (Y/N)")
    @Column("rev_info_yn")
    val reserveYn: String = "N",

    @field:Schema(description = "예약일자 yyyyMMdd")
    @Column("rev_info_date")
    val reserveDate: String? = null,

    @field:Schema(description = "예약시간 HHmm")
    @Column("rev_info_time")
    val reserveTime: String? = null,

    @field:Schema(description = "템플릿 ID")
    @Column("template_id")
    val templateId: String? = null,

    @field:Schema(description = "배너 ID")
    @Column("banner_id")
    val bannerId: String? = null,

    @field:Schema(description = "회원 아이디 (문자형)")
    @Column("user_id")
    val userId: String? = null,

    @field:Schema(description = "발급방식 (Y:핀번호수신, N:MMS, I:이미지수신)")
    @Column("gubun")
    val gubun: String = "I",

    @field:Schema(description = "응답 결과 원문")
    val response: String? = null,

    @field:Schema(description = "응답 코드")
    val code: String? = null,

    @field:Schema(description = "응답 메시지")
    val message: String? = null,

    @field:Schema(description = "PIN 번호")
    @Column("pin_no")
    val pinNo: String? = null,

    @field:Schema(description = "쿠폰 이미지 URL")
    @Column("coupon_img_url")
    val couponImgUrl: String? = null,

    @field:Schema(description = "메모사항")
    val memo: String? = null,

    @field:Schema(description = "발급 상태")
    val status: Int = 0,

    @field:Schema(description = "등록 일시")
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정 일시")
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제 일시")
    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)
