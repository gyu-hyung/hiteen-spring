package kr.jiasoft.hiteen.feature.article.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.article.domain.ArticleAssetEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ArticleAssetRepository : CoroutineCrudRepository<ArticleAssetEntity, Long> {

    @Query("""
        SELECT *
        FROM article_assets
        WHERE article_id = :articleId
        ORDER BY asset_type ASC, seq ASC, id ASC
    """)
    fun findAllByArticleId(articleId: Long): Flow<ArticleAssetEntity>

    @Query("""
        SELECT *
        FROM article_assets
        WHERE article_id = :articleId AND asset_type = :assetType
        ORDER BY seq ASC, id ASC
    """)
    fun findAllByArticleIdAndAssetType(articleId: Long, assetType: String): Flow<ArticleAssetEntity>

    @Query("""
        SELECT *
        FROM article_assets
        WHERE article_id = ANY(:articleIds)
        ORDER BY article_id ASC, asset_type ASC, seq ASC, id ASC
    """)
    fun findAllByArticleIdIn(articleIds: Array<Long>): Flow<ArticleAssetEntity>

    @Modifying
    @Query("DELETE FROM article_assets WHERE article_id = :articleId")
    suspend fun deleteByArticleId(articleId: Long)

    @Modifying
    @Query("DELETE FROM article_assets WHERE article_id = :articleId AND asset_type = :assetType")
    suspend fun deleteByArticleIdAndAssetType(articleId: Long, assetType: String)

    @Modifying
    @Query("DELETE FROM article_assets WHERE article_id = :articleId AND uid = ANY(:uids)")
    suspend fun deleteByArticleIdAndUidIn(articleId: Long, uids: Array<UUID>): Int

    @Modifying
    @Query("DELETE FROM article_assets WHERE article_id = :articleId AND asset_type = :assetType AND uid = ANY(:uids)")
    suspend fun deleteByArticleIdAndAssetTypeAndUidIn(articleId: Long, assetType: String, uids: Array<UUID>): Int

    @Modifying
    @Query("DELETE FROM article_assets WHERE article_id = :articleId AND uid = :uid")
    suspend fun deleteByArticleIdAndUid(articleId: Long, uid: UUID)

    @Query("""
        SELECT uid
        FROM article_assets
        WHERE article_id = :articleId AND asset_type = :assetType
        ORDER BY seq ASC, id ASC
        LIMIT 1
    """)
    suspend fun findFirstUidByArticleIdAndAssetType(articleId: Long, assetType: String): UUID?

    @Query("""
        SELECT COALESCE(MAX(seq), 0)
        FROM article_assets
        WHERE article_id = :articleId AND asset_type = :assetType
    """)
    suspend fun findMaxSeqByArticleIdAndAssetType(articleId: Long, assetType: String): Int
}
