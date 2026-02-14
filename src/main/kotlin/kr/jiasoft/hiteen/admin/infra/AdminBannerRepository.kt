package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminBannerResponse
import kr.jiasoft.hiteen.feature.banner.domain.BannerEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminBannerRepository : CoroutineCrudRepository<BannerEntity, Long> {
    @Query("""
        SELECT COUNT(*)
        FROM banners AS b
        WHERE
            b.deleted_at IS NULL
            AND (
                :type IS NULL OR :type = 'ALL'
                OR b.category = :type
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR b.status = :status
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        b.title ILIKE '%' || :search || '%'
                        OR b.link_url ILIKE '%' || :search || '%'
                    WHEN :searchType = 'TITLE' THEN
                        b.title ILIKE '%' || :search || '%'
                    WHEN :searchType = 'LINK' THEN
                        b.link_url ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
            ) 
    """)
    suspend fun countBanners(
        type: String?,
        status: String?,
        searchType: String?,
        search: String?,
    ): Int

    @Query("""
        SELECT b.*
        FROM banners AS b
        WHERE
            b.deleted_at IS NULL
            AND (
                :type IS NULL OR :type = 'ALL'
                OR b.category = :type
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR b.status = :status
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        b.title ILIKE '%' || :search || '%'
                        OR b.link_url ILIKE '%' || :search || '%'
                    WHEN :searchType = 'TITLE' THEN
                        b.title ILIKE '%' || :search || '%'
                    WHEN :searchType = 'LINK' THEN
                        b.link_url ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
            ) 
        ORDER BY b.id DESC
        LIMIT :size OFFSET :offset
    """)
    suspend fun listBanners(
        type: String?,
        status: String?,
        searchType: String?,
        search: String?,
        size: Int,
        offset: Int,
    ): Flow<AdminBannerResponse>
}
