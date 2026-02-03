package kr.jiasoft.hiteen.admin.services

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kr.jiasoft.hiteen.admin.dto.AdminChatMessageResponse
import kr.jiasoft.hiteen.admin.dto.EmojiInfo
import kr.jiasoft.hiteen.admin.infra.AdminChatMessageRepository
import kr.jiasoft.hiteen.admin.infra.AdminChatRoomRepository
import kr.jiasoft.hiteen.admin.infra.AdminChatUserRepository
import kr.jiasoft.hiteen.common.dto.ApiPage
import kr.jiasoft.hiteen.common.dto.PageUtil
import kr.jiasoft.hiteen.feature.chat.infra.ChatMessageAssetRepository
import kr.jiasoft.hiteen.feature.code.infra.CodeRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AdminChatService(
    private val rooms: AdminChatRoomRepository,
    private val chatUsers: AdminChatUserRepository,
    private val messages: AdminChatMessageRepository,
    private val messageAssets: ChatMessageAssetRepository,
    private val codeRepository: CodeRepository,
) {
    // 채팅 메세지 목록
    suspend fun listMessages(
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        page: Int,
        perPage: Int,
    ): ApiPage<AdminChatMessageResponse> {
        val page = page.coerceAtLeast(1)
        val perPage = perPage.coerceIn(1, 100)
        val offset = (page - 1) * perPage
        val search = search?.trim()?.takeIf { it.isNotBlank() }

        val total = messages.countBySearch(status, startDate, endDate, searchType, search)
        val rows = messages.listBySearch(status, startDate, endDate, searchType, search, perPage, offset).toList()

        val messageIds = rows.map { it.id }
        val assetsMap = messageAssets.findAllByMessageIdIn(messageIds).toList()
            .groupBy { it.messageId }
            .mapValues { (_, assets) ->
                assets.map { a -> a.uid }
            }

        val data = rows.map { row ->
            var emojiList: List<EmojiInfo>? = null
            // 단일 이모지 → list로 감싸서 통일
            if ((row.kind == 1 || (row.kind == 3 && row.content.isNullOrEmpty())) && row.emojiCode != null) {
                val emoji = row.emojiCount?.let { emojiListByCode(row.emojiCode, it) }
                emojiList = emoji?.let { listOf(it) }
            } else if (row.kind == 3 && !row.content.isNullOrEmpty()) {
                val regex = Regex("""(\S+)\s*x(\d+)""")
                val matches = regex.findAll(row.content).toList()
                val emojis = mutableListOf< EmojiInfo>()
                for (match in matches) {
                    val col = match.groupValues[1].trim()
                    val count = match.groupValues[2].toInt()
                    val emoji = emojiListByContent(col, count)
                    if (emoji != null) {
                        emojis.add(emoji)
                    }
                }

                emojiList = emojis.ifEmpty { null }
            }

            AdminChatMessageResponse(
                id = row.id,
                uid = row.uid,
                roomId = row.roomId,
                roomUid = row.roomUid,
                roomName = row.roomName,
                userId = row.userId,
                userName = row.userName,
                userPhone = row.userPhone,
                kind = row.kind,
                content = row.content,
                emojiCode = row.emojiCode,
                emojiCount = row.emojiCount,
                emojiList = emojiList,
                userCount = row.userCount,
                createdAt = row.createdAt,
                createdDate = row.createdDate,
                deletedAt = row.deletedAt,
                deletedDate = row.deletedDate,
                assets = assetsMap[row.id] ?: emptyList(),
            )
        }

        return PageUtil.of(
            items = data,
            total = total,
            page = page,
            size = perPage,
        )
    }

    suspend fun emojiListByContent(col: String, count: Int): EmojiInfo? {
        codeRepository.findByGroup("EMOJI").asFlow()
            .firstOrNull { it.col2 == col }
            ?.let {
                return EmojiInfo(
                    code = it.code,
                    col = col,
                    assetUid = it.assetUid,
                    count
                )
            }
            return null
    }

    suspend fun emojiListByCode(code: String, count: Int): EmojiInfo? {
        codeRepository.findByGroup("EMOJI").asFlow()
            .firstOrNull { it.code == code }
            ?.let {
                return EmojiInfo(
                    code,
                    col = it.col2,
                    assetUid = it.assetUid,
                    count
                )
            }
            return null
   }
}
