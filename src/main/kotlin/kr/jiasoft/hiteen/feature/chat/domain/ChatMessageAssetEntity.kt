package kr.jiasoft.hiteen.feature.chat.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("chat_messages_assets")
data class ChatMessageAssetEntity(
    @Id
    val id: Long = 0,
    val uid: UUID,
    @Column("message_id")
    val messageId: Long,
    val width: Int? = null,
    val height: Int? = null,
)