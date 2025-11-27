package kr.jiasoft.hiteen.feature.mbti.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("mbti")
data class MbtiResultEntity(

    @Id
    val id: Long? = null,

    @Column("user_id")
    val userId: Long,

    @Column("rates")
    val rates: String,

    @Column("mbti")
    val mbti: String,

    @Column("created_at")
    val createdAt: OffsetDateTime? = null,

    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,
)
