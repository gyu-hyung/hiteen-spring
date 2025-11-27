package kr.jiasoft.hiteen.feature.batch.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("batch_history")
data class BatchHistoryEntity(

    @Id
    val id: Long? = null,

    @Column("job_name")
    val jobName: String,

    @Column("status")
    val status: String, // STARTED / SUCCESS / FAILED / SKIPPED

    @Column("started_at")
    val startedAt: OffsetDateTime,

    @Column("finished_at")
    val finishedAt: OffsetDateTime? = null,

    @Column("duration_ms")
    val durationMs: Long? = null,

    @Column("error_message")
    val errorMessage: String? = null,
)
