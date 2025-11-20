package kr.jiasoft.hiteen.feature.goods.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("goods_category")
@Schema(description = "기프티쇼 카테고리 엔티티")
data class GoodsCategoryEntity(

    @Id
    @field:Schema(description = "카테고리 PK")
    val id: Long = 0,

    @field:Schema(description = "카테고리 일련번호 (Unique)")
    val seq: Int,

    @field:Schema(description = "카테고리명")
    val name: String,

    @field:Schema(description = "정렬 순서")
    val sort: Int = 9999,

    @field:Schema(description = "삭제 여부 (0: 정상, 1: 삭제)")
    @Column("del_yn")
    val delYn: Int = 0,

    @field:Schema(description = "상태 (0: 비노출, 1: 노출)")
    val status: Int = 0,

    @field:Schema(description = "생성일시")
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정일시")
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제일시")
    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)
