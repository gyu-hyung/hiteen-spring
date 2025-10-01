package kr.jiasoft.hiteen.feature.play.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("games")
data class GameEntity(
    @Id
    val id: Long = 0L,
    val code: String,          // "REACTION", "TYPING"
    val name: String,
    val description: String? = null,
    val status: String = "ACTIVE",
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime? = null,
    val deletedAt: OffsetDateTime? = null,
)
