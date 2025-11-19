package kr.jiasoft.hiteen.feature.gift.app

import io.r2dbc.postgresql.codec.Json
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.threeten.bp.OffsetDateTime
import java.util.UUID

@Table("gift")
data class GiftEntity (
    @Id
    @field:Schema(description = "선물 ID")
    val id: Long = 0,
    @field:Schema(description = "선물 UID")
    val uid: UUID = UUID.randomUUID(),

    @field:Schema(description = "상품 구분 POINT, VOUCHER, DELIVERY, ETC")
    val type: String,

    @field:Schema(description = "선물 분류 CHALLENGE, ADMIN")
    val category: String,

    @field:Schema(description = "회원번호")
    val userId: Long,

    @field:Schema(description = "결제번호")
    val payId: Long,

    @field:Schema(description = "선물 메모")
    val memo: String,

    @field:Schema(description = "받은 회원목록")
    val users: Json,

    @field:Schema(description = "생성일시")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정일시")
    val updatedAt: OffsetDateTime,

    @field:Schema(description = "삭제일시")
    val deletedAt: OffsetDateTime?
)