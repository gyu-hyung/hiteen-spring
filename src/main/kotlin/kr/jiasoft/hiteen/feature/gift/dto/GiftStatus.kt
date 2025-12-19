package kr.jiasoft.hiteen.feature.gift.dto

enum class GiftStatus (
    val code: Int,
    val description: String,
) {
    WAIT(0, "대기"),
    SENT(1, "발송 완료"),
    USED(2, "사용 완료"),
    EXPIRED(3, "만료"),
    DELIVERY_REQUESTED(4, "배송 요청"),
    DELIVERY_DONE(5, "배송 완료"),
    GRANT_REQUESTED(6, "지급 요청"),
    GRANTED(7, "지급 완료")
    ;

    companion object {
        fun from(code: Int): GiftStatus =
            entries.find { it.code == code } ?: error("Unknown status code: $code")
    }
}