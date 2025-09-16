package kr.jiasoft.hiteen.feature.chat.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("chat_rooms")
data class ChatRoomEntity(

    @Id
    val id: Long = 0,

    val uid: UUID = UUID.randomUUID(),

    @Column("last_user_id")
    val lastUserId: Long? = null,

    @Column("last_message_id")
    val lastMessageId: Long? = null,

    @Column("created_id")
    val createdId: Long,

    @Column("created_at")
    val createdAt: OffsetDateTime,

    @Column("updated_id")
    val updatedId: Long? = null,

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @Column("deleted_id")
    val deletedId: Long? = null,

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,

)