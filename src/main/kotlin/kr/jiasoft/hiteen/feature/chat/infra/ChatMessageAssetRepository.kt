package kr.jiasoft.hiteen.feature.chat.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.chat.domain.ChatMessageAssetEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface ChatMessageAssetRepository : CoroutineCrudRepository<ChatMessageAssetEntity, Long> {
    @Query("SELECT * FROM chat_messages_assets WHERE message_id=:messageId")
    fun listByMessage(messageId: Long): Flow<ChatMessageAssetEntity>

    fun findAllByMessageIdIn(messageIds: Collection<Long>): Flow<ChatMessageAssetEntity>
}