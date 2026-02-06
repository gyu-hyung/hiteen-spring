package kr.jiasoft.hiteen.feature.article.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("article_assets")
data class ArticleAssetEntity(
    @Id
    val id: Long = 0,
    val articleId: Long,
    val uid: UUID,
    val assetType: String = ArticleAssetType.ATTACHMENT.name,
    val seq: Int = 0,
)

