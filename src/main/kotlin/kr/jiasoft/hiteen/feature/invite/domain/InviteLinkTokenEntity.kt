package kr.jiasoft.hiteen.feature.invite.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("invite_link_tokens")
data class InviteLinkTokenEntity(
    @Id
    val id: Long = 0,

    @Column("token")
    val token: String,

    @Column("code")
    val code: String,

    @Column("expires_at")
    val expiresAt: OffsetDateTime,

    @Column("used_at")
    val usedAt: OffsetDateTime? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column("used_by")
    val usedBy: Long? = null,
)

