package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.cash.domain.CashRuleEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminCashRuleRepository : CoroutineCrudRepository<CashRuleEntity, Long> {

    @Query(
        """
        SELECT COUNT(*)
        FROM cash_rules
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
        FROM cash_rules
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
    ): List<CashRuleEntity>

    @Query(
        """
        SELECT *
        FROM cash_rules
        WHERE id = :id
          AND (:includeDeleted = TRUE OR deleted_at IS NULL)
        LIMIT 1
        """
    )
    suspend fun findByIdFiltered(id: Long, includeDeleted: Boolean): CashRuleEntity?

    @Query(
        """
        SELECT *
        FROM cash_rules
        WHERE action_code = :actionCode
        LIMIT 1
        """
    )
    suspend fun findAnyByActionCode(actionCode: String): CashRuleEntity?

    @Modifying
    @Query(
        """
        UPDATE cash_rules
        SET deleted_at = now()
        WHERE id = :id
          AND deleted_at IS NULL
        """
    )
    suspend fun softDelete(id: Long): Int

    @Modifying
    @Query(
        """
        UPDATE cash_rules
        SET deleted_at = NULL
        WHERE id = :id
        """
    )
    suspend fun restore(id: Long): Int
}

