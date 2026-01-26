package kr.jiasoft.hiteen.feature.code.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("codes")
data class CodeEntity(
    @Id
    val id: Long = 0,

    @Column("code_name")
    val codeName: String,

    @Column("code")
    val code: String,

    @Column("code_group_name")
    val codeGroupName: String,

    @Column("code_group")
    val codeGroup: String,

    @Column("status")
    val status: CodeStatus = CodeStatus.INACTIVE,

    @Column("asset_uid")
    val assetUid: UUID? = null,

    @Column("col1")
    val col1: String? = null,

    @Column("col2")
    val col2: String? = null,

    @Column("col3")
    val col3: String? = null,

    @Column("created_id")
    val createdId: Long? = null,

    @Column("created_at")
    val createdAt: OffsetDateTime,

    @Column("updated_id")
    val updatedId: Long? = null,

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @Column("deleted_id")
    val deletedId: Long? = null,

    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,

)
