package kr.jiasoft.hiteen.feature.pin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table(name = "pin_users")
data class PinUsersEntity(
    @Id
    val id: Long? = null,

    @Column("pin_id")
    val pinId: Long,

    @Column("user_id")
    val userId: Long,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
