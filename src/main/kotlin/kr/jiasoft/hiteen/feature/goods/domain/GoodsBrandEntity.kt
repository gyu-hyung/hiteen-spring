package kr.jiasoft.hiteen.feature.goods.domain

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("goods_brand")
@Schema(description = "기프티쇼 브랜드 정보 엔티티")
data class GoodsBrandEntity(

    @Id
    @field:Schema(description = "브랜드 ID")
    val id: Long = 0,

    @field:Schema(description = "브랜드 일련번호")
    @Column("brand_seq")
    val brandSeq: Int,

    @field:Schema(description = "브랜드 코드 (Unique)")
    @Column("brand_code")
    val brandCode: String,

    @field:Schema(description = "브랜드명")
    @Column("brand_name")
    val brandName: String,

    @field:Schema(description = "웹 배너 이미지 URL")
    @Column("brand_banner_img")
    val brandBannerImg: String? = null,

    @field:Schema(description = "브랜드 아이콘 이미지 URL")
    @Column("brand_icon_img")
    val brandIconImg: String? = null,

    @field:Schema(description = "브랜드 썸네일 이미지 URL")
    @Column("mms_thum_img")
    val mmsThumbImg: String? = null,

    @field:Schema(description = "브랜드 설명")
    val content: String? = null,

    @field:Schema(description = "1차 카테고리 ID")
    @Column("category1_seq")
    val category1Seq: Int? = null,

    @field:Schema(description = "1차 카테고리 이름")
    @Column("category1_name")
    val category1Name: String? = null,

    @field:Schema(description = "2차 카테고리 ID")
    @Column("category2_seq")
    val category2Seq: Int? = null,

    @field:Schema(description = "2차 카테고리 이름")
    @Column("category2_name")
    val category2Name: String? = null,

    @field:Schema(description = "신규 여부 (Y/N)")
    @Column("new_flag")
    val newFlag: String? = null,

    @field:Schema(description = "정렬 번호")
    val sort: Int? = null,

    @field:Schema(description = "삭제 여부 (0/1)")
    @Column("del_yn")
    val delYn: Int,

    @field:Schema(description = "노출 여부 (0/1)")
    val status: Int,

    @field:Schema(description = "등록 일시")
    @Column("created_at")
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @field:Schema(description = "수정 일시")
    @Column("updated_at")
    val updatedAt: OffsetDateTime? = null,

    @field:Schema(description = "삭제 일시")
    @Column("deleted_at")
    val deletedAt: OffsetDateTime? = null,
)
