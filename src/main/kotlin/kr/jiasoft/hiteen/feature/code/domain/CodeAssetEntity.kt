package kr.jiasoft.hiteen.feature.code.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("code_assets")
data class CodeAssetEntity(
    @Id
    val id: Long = 0,
    val codeId: Long,
    val uid: UUID,
    val createdId: Long,
    val createdAt: OffsetDateTime,
)