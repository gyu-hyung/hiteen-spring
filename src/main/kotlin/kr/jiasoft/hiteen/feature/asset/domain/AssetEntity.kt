package kr.jiasoft.hiteen.feature.asset.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table("assets")
data class AssetEntity(
    @Id
    val id: Long? = null,

    val uid: UUID? = UUID.randomUUID(),

    val originFileName: String? = null,

    val storeFileName: String? = null,

    val filePath: String? = null,   // 디스크상의 실제 경로(상대/절대)

    val type: String? = null,       // mime-type 같은 용도

    val size: Long? = null,         // bytes

    val width: Int? = null,

    val height: Int? = null,

    val ext: String? = null,        // 확장자(소문자)

    val downloadCount: Int? = 0,

    val createdId: Long,

    val createdAt: OffsetDateTime? = null,

    val updatedId: Long? = null,

    val updatedAt: OffsetDateTime? = null,

    val deletedId: Long? = null,

    val deletedAt: OffsetDateTime? = null,
)