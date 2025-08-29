package kr.jiasoft.hiteen.feature.chat.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("chat_users")
data class ChatUserEntity(

    @Id
    val id: Long? = null,

    @Column("chat_room_id")
    val chatRoomId: Long,

    @Column("user_id")
    val userId: Long,

    val status: Short? = 0,       // 0=정상, 1=뮤트, 2=차단...

    val push: Boolean? = true,

    @Column("push_at")
    val pushAt: OffsetDateTime? = null,

    @Column("joining_at")
    val joiningAt: OffsetDateTime? = null,

    @Column("leaving_at")
    val leavingAt: OffsetDateTime? = null,

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,

    @Column("last_read_message_id")
    val lastReadMessageId: Long? = null,

    @Column("last_read_at")
    val lastReadAt: OffsetDateTime? = null,
)