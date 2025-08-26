package kr.jiasoft.hiteen.feature.board.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("board_assets")
data class BoardAssetEntity (
    @Id
    val id: Long? = null,
    val boardId: Long,
    val uid: UUID,
)