package kr.jiasoft.hiteen.feature.chat.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("chat_messages")
data class ChatMessageEntity(

    @Id
    val id: Long = 0,

    @Column("chat_room_id")
    val chatRoomId: Long,

    @Column("user_id")
    val userId: Long,

    val uid: UUID = UUID.randomUUID(),

    val content: String? = null,

//    @Column("read_count")
//    val readCount: Int? = 0,

    val kind: Int = 0,// 0: 텍스트, 1: 이모지

    @Column("emoji_code")
    val emojiCode: String? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime,

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,

)