package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.point.domain.PointRuleEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminPointRuleRepository : CoroutineCrudRepository<PointRuleEntity, Long> {

    @Query(
        """
        SELECT COUNT(*)
        FROM point_rules
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND deleted_at IS NULL)
                OR (:status = 'DELETED' AND deleted_at IS NOT NULL)
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ACTION_CODE' THEN
                        action_code ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'DESCRIPTION' THEN
                        COALESCE(description, '') ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
        """
    )
    suspend fun countList(
        search: String?,
        searchType: String,
        status: String,
    ): Int

    @Query(
        """
        SELECT *
        FROM point_rules
        WHERE
            (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND deleted_at IS NULL)
                OR (:status = 'DELETED' AND deleted_at IS NOT NULL)
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ACTION_CODE' THEN
                        action_code ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'DESCRIPTION' THEN
                        COALESCE(description, '') ILIKE ('%' || :search || '%')
                    ELSE TRUE
                END
            )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN id END ASC,
            CASE WHEN :order = 'DESC' THEN id END DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun list(
        search: String?,
        searchType: String,
        status: String,
        order: String,
        limit: Int,
        offset: Int,
    ): List<PointRuleEntity>

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
