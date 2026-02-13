package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminExpActionResponse
import kr.jiasoft.hiteen.feature.level.domain.ExpActionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminExpActionRepository : CoroutineCrudRepository<ExpActionEntity, Long> {
    @Query(
        """
        SELECT COUNT(*)
        FROM exp_actions AS e
        WHERE
            (
                :status IS NULL
                OR e.enabled = :status
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        e.action_code ILIKE '%' || :search || '%'
                        OR e.description ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CODE' THEN
                        e.action_code ILIKE '%' || :search || '%'
                    WHEN :searchType = 'DESCRIPTION' THEN
                        e.description ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
            )
    """
    )
    suspend fun totalCount(
        status: Boolean?,
        searchType: String?,
        search: String?,
    ): Int

    @Query(
        """
        SELECT *
        FROM exp_actions AS e
        WHERE
            (
                :status IS NULL
                OR e.enabled = :status
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        e.action_code ILIKE '%' || :search || '%'
                        OR e.description ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CODE' THEN
                        e.action_code ILIKE '%' || :search || '%'
                    WHEN :searchType = 'DESCRIPTION' THEN
                        e.description ILIKE '%' || :search || '%'
                    ELSE TRUE
                END
            )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN e.action_code END ASC,
            CASE WHEN :order = 'DESC' THEN e.action_code END DESC
        LIMIT :size OFFSET :offset
    """
    )
    suspend fun listByPage(
        status: Boolean?,
        searchType: String?,
        search: String?,
        order: String,
        size: Int,
        offset: Int,
    ): Flow<AdminExpActionResponse>
}
