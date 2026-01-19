package kr.jiasoft.hiteen.feature.level.domain

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

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
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val createdAt: OffsetDateTime? = null,

    @Column("updated_at")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val updatedAt: OffsetDateTime? = null,
)

