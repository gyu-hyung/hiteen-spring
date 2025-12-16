package kr.jiasoft.hiteen.feature.gift.domain

import io.r2dbc.postgresql.codec.Json
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("gift")
@Schema(description = "선물 내역 엔티티")
data class GiftEntity (

    @Id
    @field:Schema(description = "선물 고유 ID")
    val id: Long = 0,

    @field:Schema(description = "선물 고유 UID")
    val uid: UUID = UUID.randomUUID(),

    @field:Schema(description = "선물 구분 (Point, Voucher, Delivery, GiftCard)")
    val type: GiftType? = null,

    @field:Schema(description = "카테고리(Join, Challenge, Admin, Event)")
    val category: GiftCategory,

    @field:Schema(description = "회원 번호")
    @Column("user_id")
    val userId: Long,

//    @field:Schema(description = "결제 번호")
//    @Column("pay_id")
//    val payId: Long? = null,

    @field:Schema(description = "메모")
    val memo: String? = null,

    @field:Schema(description = "받은 사용자 목록 JSON")
    val users: Json? = null,

    @field:Schema(description = "생성일시")
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정일시")
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제일시")
    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)