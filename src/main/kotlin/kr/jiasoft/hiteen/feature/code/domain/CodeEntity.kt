package kr.jiasoft.hiteen.feature.code.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("codes")
data class CodeEntity(
    @Id
    val id: Long = 0,
    val codeName: String,
    val code: String,
    val codeGroupName: String,
    val codeGroup: String,
    val status: CodeStatus? = CodeStatus.INACTIVE,
    val createdId: Long,
    val createdAt: OffsetDateTime,
    val updatedId: Long? = null,
    val updatedAt: OffsetDateTime? = null,
    val deletedId: Long? = null,
    val deletedAt: OffsetDateTime? = null,
)
