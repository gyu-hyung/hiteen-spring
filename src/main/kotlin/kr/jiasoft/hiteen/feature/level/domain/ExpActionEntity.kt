package kr.jiasoft.hiteen.feature.level.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("exp_actions")
data class ExpActionEntity(
    @Id
    val id: Long? = null,

    @Column("action_code")
    val actionCode: String,

    @Column("description")
    val description: String,

    @Column("points")
    val points: Int,

    @Column("daily_limit")
    val dailyLimit: Int? = null,

    @Column("enabled")
    val enabled: Boolean = true,

    @Column("created_at")
    val createdAt: LocalDateTime? = null,

    @Column("updated_at")
    val updatedAt: LocalDateTime? = null,
)

