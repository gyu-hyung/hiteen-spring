package kr.jiasoft.hiteen.feature.gift.app

import io.r2dbc.postgresql.codec.Json
import org.threeten.bp.OffsetDateTime
import java.util.UUID

data class GiftEntity (
    val id: Long = 0,
    val uid: UUID = UUID.randomUUID(),
    val type: String,
    val category: String,
    val userId: Long,
    val payId: Long,
    val memo: String,
    val users: Json,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?
)