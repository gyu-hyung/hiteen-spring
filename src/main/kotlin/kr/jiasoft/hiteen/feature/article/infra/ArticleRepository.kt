package kr.jiasoft.hiteen.feature.article.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.article.domain.ArticleEntity
import kr.jiasoft.hiteen.feature.article.dto.ArticleNavigation
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : CoroutineCrudRepository<ArticleEntity, Long> {

    @Query("""
        SELECT a.*
        FROM articles a
        WHERE a.id = :id
          AND a.deleted_at IS NULL
    """)
    suspend fun findByIdAndNotDeleted(id: Long): ArticleEntity?

    @Modifying
    @Query("""
        UPDATE articles
        SET hits = hits + 1
        WHERE id = :id
    """)
    suspend fun increaseHits(id: Long)

    @Query("""
        SELECT a.*
        FROM articles a
        WHERE a.deleted_at IS NULL
          AND (:category IS NULL OR a.category = :category)
          AND (:search IS NULL OR a.subject ILIKE CONCAT('%', :search, '%') OR a.content ILIKE CONCAT('%', :search, '%'))
          AND (
            CASE 
              WHEN :status IS NULL THEN a.status IN ('ACTIVE', 'ENDED', 'WINNER_ANNOUNCED')
              WHEN :status = 'ACTIVE' THEN a.status = 'ACTIVE' AND (a.end_date IS NULL OR a.end_date >= CURRENT_DATE)
              WHEN :status = 'ENDED' THEN a.status = 'ENDED' OR (a.status = 'ACTIVE' AND a.end_date IS NOT NULL AND a.end_date < CURRENT_DATE)
              WHEN :status = 'WINNER_ANNOUNCED' THEN a.status = 'WINNER_ANNOUNCED'
              ELSE a.status = :status
            END
          )
          AND (a.start_date IS NULL OR a.start_date <= CURRENT_DATE)
        ORDER BY a.created_at DESC
        LIMIT :size OFFSET :offset
    """)
    fun searchByPage(
        category: String?,
        search: String?,
        status: String?,
        size: Int,
        offset: Int,
    ): Flow<ArticleEntity>

    @Query("""
        SELECT COUNT(*)
        FROM articles a
        WHERE a.deleted_at IS NULL
          AND (:category IS NULL OR a.category = :category)
          AND (:search IS NULL OR a.subject ILIKE CONCAT('%', :search, '%') OR a.content ILIKE CONCAT('%', :search, '%'))
          AND (
            CASE 
              WHEN :status IS NULL THEN a.status IN ('ACTIVE', 'ENDED', 'WINNER_ANNOUNCED')
              WHEN :status = 'ACTIVE' THEN a.status = 'ACTIVE' AND (a.end_date IS NULL OR a.end_date >= CURRENT_DATE)
              WHEN :status = 'ENDED' THEN a.status = 'ENDED' OR (a.status = 'ACTIVE' AND a.end_date IS NOT NULL AND a.end_date < CURRENT_DATE)
              WHEN :status = 'WINNER_ANNOUNCED' THEN a.status = 'WINNER_ANNOUNCED'
              ELSE a.status = :status
            END
          )
          AND (a.start_date IS NULL OR a.start_date <= CURRENT_DATE)
    """)
    suspend fun countByCategory(
        category: String?,
        search: String?,
        status: String?,
    ): Int

    @Query("""
        SELECT a.*
        FROM articles a
        WHERE a.deleted_at IS NULL
          AND (:category IS NULL OR a.category = :category)
          AND (:search IS NULL OR a.subject ILIKE CONCAT('%', :search, '%') OR a.content ILIKE CONCAT('%', :search, '%'))
          AND (
            CASE 
              WHEN :status IS NULL THEN a.status IN ('ACTIVE', 'ENDED', 'WINNER_ANNOUNCED')
              WHEN :status = 'ACTIVE' THEN a.status = 'ACTIVE' AND (a.end_date IS NULL OR a.end_date >= CURRENT_DATE)
              WHEN :status = 'ENDED' THEN a.status = 'ENDED' OR (a.status = 'ACTIVE' AND a.end_date IS NOT NULL AND a.end_date < CURRENT_DATE)
              WHEN :status = 'WINNER_ANNOUNCED' THEN a.status = 'WINNER_ANNOUNCED'
              ELSE a.status = :status
            END
          )
          AND (a.start_date IS NULL OR a.start_date <= CURRENT_DATE)
          AND (
              :cursorId IS NULL 
              OR a.id < :cursorId
          )
        ORDER BY a.created_at DESC, a.id DESC
        LIMIT :size
    """)
    fun searchByCursor(
        category: String?,
        search: String?,
        status: String?,
        size: Int,
        cursorId: Long?,
    ): Flow<ArticleEntity>

    /**
     * 이전글 조회 (현재 글보다 이전에 작성된 글 중 가장 최근 글)
     */
    @Query("""
        SELECT a.id, a.subject, a.created_at
        FROM articles a
        WHERE a.deleted_at IS NULL
          AND a.category = :category
          AND a.status = 'ACTIVE'
          AND (a.start_date IS NULL OR a.start_date <= CURRENT_DATE)
          AND (a.end_date IS NULL OR a.end_date >= CURRENT_DATE)
          AND a.created_at < (SELECT created_at FROM articles WHERE id = :currentId)
        ORDER BY a.created_at DESC
        LIMIT 1
    """)
    suspend fun findPreviousArticle(currentId: Long, category: String): ArticleNavigation?

    /**
     * 다음글 조회 (현재 글보다 이후에 작성된 글 중 가장 오래된 글)
     */
    @Query("""
        SELECT a.id, a.subject, a.created_at
        FROM articles a
        WHERE a.deleted_at IS NULL
          AND a.category = :category
          AND a.status = 'ACTIVE'
          AND (a.start_date IS NULL OR a.start_date <= CURRENT_DATE)
          AND (a.end_date IS NULL OR a.end_date >= CURRENT_DATE)
          AND a.created_at > (SELECT created_at FROM articles WHERE id = :currentId)
        ORDER BY a.created_at ASC
        LIMIT 1
    """)
    suspend fun findNextArticle(currentId: Long, category: String): ArticleNavigation?
}

