package kr.jiasoft.hiteen.feature.map.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("api_keys")
data class ApiKeyEntity(
    @Id
    val id: Long? = null,

    val type: String,

    @Column("api_key_id")
    val apiKeyId: String,

    @Column("api_key")
    val apiKey: String,

    @Column("max_count")
    val maxCount: Long? = 0,

    @Column("use_count")
    val useCount: Long? = 0,

    @Column("month_count")
    val monthCount: Long? = 0,

    @Column("response_code")
    val responseCode: String? = null,

    val status: String = "ACTIVE",

    @Column("created_at")
    val createdAt: OffsetDateTime? = null,

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)

