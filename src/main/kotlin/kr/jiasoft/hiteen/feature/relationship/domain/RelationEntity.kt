package kr.jiasoft.hiteen.feature.relationship.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.threeten.bp.OffsetDateTime

@Table("relation_history")
data class RelationEntity(

    @Id
    val id: Long? = null,

    val userId: Long,

    val targetId: Long,

    val relationType: String,

    val action: String,

    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    val updatedAt: OffsetDateTime? = null,
)
