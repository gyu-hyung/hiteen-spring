package kr.jiasoft.hiteen.feature.gift_v2.domain

enum class VoucherStatus(
    val code: String,
    val description: String
) {

    /** 01 */
    ISSUED("01", "발행"),

    /** 02 */
    EXCHANGED_USED("02", "교환(사용완료)"),

    /** 03 */
    RETURNED("03", "반품"),

    /** 04 */
    CANCELLED_BY_ADMIN("04", "관리폐기"),

    /** 05 */
    CANCELED("05", "환불"),

    /** 06 */
    REISSUED("06", "재발행"),

    /** 07 */
    PURCHASE_CANCEL("07", "구매취소(폐기)"),

    /** 08 */
    EXPIRED("08", "기간만료"),

    /** 09 */
    RECEIVED("09", "바우쳐(발송)"),

    /** 10 */
    INVALID("10", "잔액없음"),

    /** 11 */
    BALANCE_AVAILABLE("11", "잔액기간만료"),

    /** 12 */
    PERIOD_CANCEL("12", "기간만료취소"),

    /** 13 */
    EXCHANGE_PENDING("13", "환전"),

    /** 14 */
    REFUND_AVAILABLE("14", "환급"),

    /** 15 */
    BALANCE_ZERO("15", "잔액없음"),

    /** 16 */
    PERIOD_CANCEL_BALANCE("16", "잔액기간만료취소"),

    /** 21 */
    REGISTERED("21", "등록"),

    /** 22 */
    REGISTRATION_CANCEL("22", "등록취소"),

    /** 23 */
    GIFT_SELECTED("23", "선정"),

    /** 24 */
    TEMP_PAYMENT_STATUS("24", "임시발급상태");

    companion object {
        fun fromCode(code: String): VoucherStatus? =
            entries.firstOrNull { it.code == code }
    }
}
