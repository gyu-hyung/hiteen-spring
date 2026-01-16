package kr.jiasoft.hiteen.feature.board.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("board_banners")
data class BoardBannerEntity(
    @Id
    val id: Long = 0L,

    val boardId: Long,
    val uid: UUID,
    val bannerType: String,
    val seq: Int = 1,
)
