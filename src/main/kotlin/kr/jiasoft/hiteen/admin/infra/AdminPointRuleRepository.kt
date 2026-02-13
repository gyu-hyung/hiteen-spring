package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.admin.dto.AdminPointRuleResponse
import kr.jiasoft.hiteen.feature.point.domain.PointRuleEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminPointRuleRepository : CoroutineCrudRepository<PointRuleEntity, Long> {
    @Query(
        """
        SELECT COUNT(*)
        FROM point_rules AS p
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND p.deleted_at IS NULL)
                OR (:status = 'DELETED' AND p.deleted_at IS NOT NULL)
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        p.action_code ILIKE '%' || :search || '%'
                        OR p.description ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CODE' THEN
                        p.action_code ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'DESCRIPTION' THEN
                        COALESCE(p.description, '') ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
        """
    )
    suspend fun countList(
        status: String?,
        searchType: String?,
        search: String?,
    ): Int

    @Query(
        """
        SELECT *
        FROM point_rules AS p
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND p.deleted_at IS NULL)
                OR (:status = 'DELETED' AND p.deleted_at IS NOT NULL)
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        p.action_code ILIKE '%' || :search || '%'
                        OR p.description ILIKE '%' || :search || '%'
                    WHEN :searchType = 'CODE' THEN
                        p.action_code ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'DESCRIPTION' THEN
                        COALESCE(p.description, '') ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN p.action_code END ASC,
            CASE WHEN :order = 'DESC' THEN p.action_code END DESC
        LIMIT :size OFFSET :offset
        """
    )
    suspend fun list(
        status: String?,
        searchType: String?,
        search: String?,
        order: String,
        size: Int,
        offset: Int,
    ): List<AdminPointRuleResponse>

    @Query(
        """
        SELECT *
        FROM point_rules
        WHERE id = :id
          AND (:includeDeleted = TRUE OR deleted_at IS NULL)
        LIMIT 1
        """
    )
    suspend fun findByIdFiltered(id: Long, includeDeleted: Boolean): PointRuleEntity?

    @Query(
        """
        SELECT *
        FROM point_rules
        WHERE action_code = :actionCode
        LIMIT 1
        """
    )
    suspend fun findAnyByActionCode(actionCode: String): PointRuleEntity?

    @Modifying
    @Query(
        """
        UPDATE point_rules
        SET deleted_at = now()
        WHERE id = :id
          AND deleted_at IS NULL
        """
    )
    suspend fun softDelete(id: Long): Int

    @Modifying
    @Query(
        """
        UPDATE point_rules
        SET deleted_at = NULL
        WHERE id = :id
        """
    )
    suspend fun restore(id: Long): Int
}
