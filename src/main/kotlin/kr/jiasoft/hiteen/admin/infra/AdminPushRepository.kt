package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.push.domain.PushEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface AdminPushRepository : CoroutineCrudRepository<PushEntity, Long> {

    @Query(
        """
        SELECT COUNT(*)
        FROM push p
        WHERE (
            :status = 'ALL'
            OR (:status = 'ACTIVE' AND p.deleted_at IS NULL)
            OR (:status = 'DELETED' AND p.deleted_at IS NOT NULL)
        )
          AND (
            :search IS NULL
            OR (
                :searchType = 'ALL'
                OR (:searchType = 'CODE' AND COALESCE(p.code, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'TITLE' AND COALESCE(p.title, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'MESSAGE' AND COALESCE(p.message, '') ILIKE ('%' || :search || '%'))
            )
          )
        """
    )
    suspend fun countList(search: String?, searchType: String, status: String): Int

    @Query(
        """
        SELECT *
        FROM push p
        WHERE (
            :status = 'ALL'
            OR (:status = 'ACTIVE' AND p.deleted_at IS NULL)
            OR (:status = 'DELETED' AND p.deleted_at IS NOT NULL)
        )
          AND (
            :search IS NULL
            OR (
                :searchType = 'ALL'
                OR (:searchType = 'CODE' AND COALESCE(p.code, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'TITLE' AND COALESCE(p.title, '') ILIKE ('%' || :search || '%'))
                OR (:searchType = 'MESSAGE' AND COALESCE(p.message, '') ILIKE ('%' || :search || '%'))
            )
          )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN p.id END ASC,
            CASE WHEN :order = 'DESC' THEN p.id END DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun list(search: String?, searchType: String, status: String, order: String, limit: Int, offset: Int): List<PushEntity>

    @Modifying
    @Query("UPDATE push SET deleted_at = now() WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: Long): Int
}

