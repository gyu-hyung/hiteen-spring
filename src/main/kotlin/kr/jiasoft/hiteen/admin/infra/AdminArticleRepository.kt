package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.article.domain.ArticleEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminArticleRepository : CoroutineCrudRepository<ArticleEntity, Long> {

    @Query("""
        SELECT a.*
        FROM articles a
        WHERE
           a.deleted_at IS NULL
           AND
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        a.subject ILIKE CONCAT('%', :search, '%')
                        OR a.content ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'subject' AND a.subject ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'content' AND a.content ILIKE CONCAT('%', :search, '%'))
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND a.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND a.status = 'INACTIVE')
            )
    
            AND (
                :displayStatus IS NULL OR :displayStatus = 'ALL'
                OR (
                    :displayStatus = 'ACTIVE'
                    AND (a.start_date IS NULL OR a.start_date <= CURRENT_DATE)
                    AND (a.end_date IS NULL OR a.end_date >= CURRENT_DATE)
                )
                OR (
                    :displayStatus = 'INACTIVE'
                    AND (
                        (a.start_date IS NOT NULL AND a.start_date > CURRENT_DATE)
                        OR (a.end_date IS NOT NULL AND a.end_date < CURRENT_DATE)
                    )
                )
            )
    
            AND (
                :category IS NULL OR :category = 'ALL'
                OR (:category = 'NOTICE' AND a.category = 'NOTICE')
                OR (:category = 'EVENT' AND a.category = 'EVENT')
            )
    
        ORDER BY
            CASE WHEN :order = 'DESC' THEN a.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN a.created_at END ASC
    
        LIMIT :size OFFSET GREATEST((:page - 1) * :size, 0)
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        displayStatus: String?,
        category: String?,
    ): Flow<ArticleEntity>

    @Query("""
        SELECT COUNT(*)
        FROM articles a
        WHERE
           a.deleted_at IS NULL
           AND
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        a.subject ILIKE CONCAT('%', :search, '%')
                        OR a.content ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'subject' AND a.subject ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'content' AND a.content ILIKE CONCAT('%', :search, '%'))
            )
    
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND a.status = 'ACTIVE')
                OR (:status = 'INACTIVE' AND a.status = 'INACTIVE')
            )
    
            AND (
                :displayStatus IS NULL OR :displayStatus = 'ALL'
                OR (
                    :displayStatus = 'ACTIVE'
                    AND (a.start_date IS NULL OR a.start_date <= CURRENT_DATE)
                    AND (a.end_date IS NULL OR a.end_date >= CURRENT_DATE)
                )
                OR (
                    :displayStatus = 'INACTIVE'
                    AND (
                        (a.start_date IS NOT NULL AND a.start_date > CURRENT_DATE)
                        OR (a.end_date IS NOT NULL AND a.end_date < CURRENT_DATE)
                    )
                )
            )
    
            AND (
                :category IS NULL OR :category = 'ALL'
                OR (:category = 'NOTICE' AND a.category = 'NOTICE')
                OR (:category = 'EVENT' AND a.category = 'EVENT')
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        displayStatus: String?,
        category: String?,
    ): Int
}
