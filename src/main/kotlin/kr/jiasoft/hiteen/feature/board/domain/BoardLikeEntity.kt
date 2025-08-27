package kr.jiasoft.hiteen.feature.board.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime


@Table("board_likes")
data class BoardLikeEntity (
    @Id
    val id: Long? = null,
    val boardId: Long,
    val userId: Long,
    val createdAt: OffsetDateTime? = null,
//    val updatedAt: OffsetDateTime? = null,
//    val deletedAt: OffsetDateTime? = null,
)