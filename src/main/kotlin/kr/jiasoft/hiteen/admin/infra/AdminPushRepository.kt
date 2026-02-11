package kr.jiasoft.hiteen.admin.infra

import kr.jiasoft.hiteen.feature.push.domain.PushEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import java.time.LocalDateTime

interface AdminPushRepository : CoroutineCrudRepository<PushEntity, Long> {

    @Query(
        """
        SELECT COUNT(*)
        FROM push p
        WHERE
            (
                :type IS NULL OR :type = 'ALL'
                OR p.code ILIKE ('%' || :type || '%')
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND p.deleted_at IS NULL)
                OR (:status = 'DELETED' AND p.deleted_at IS NOT NULL)
            )
            AND (
                :startDate IS NULL
                OR p.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR p.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (
                            COALESCE(p.title, '') ILIKE ('%' || :search || '%')
                            OR COALESCE(p.message, '') ILIKE ('%' || :search || '%')
                            OR EXISTS (
                                SELECT 1
                                FROM push_detail pd
                                JOIN users u ON u.id = pd.user_id
                                WHERE pd.push_id = p.id
                                    AND (
                                        COALESCE(u.nickname, '') ILIKE ('%' || :search || '%')
                                        OR COALESCE(u.phone, '') ILIKE ('%' || :search || '%')
                                    )
                            )
                        )
                    WHEN :searchType = 'TITLE' THEN
                        COALESCE(p.title, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'MESSAGE' THEN
                        COALESCE(p.message, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'NICKNAME' THEN
                        EXISTS (
                            SELECT 1
                            FROM push_detail pd
                            JOIN users u ON u.id = pd.user_id
                            WHERE pd.push_id = p.id
                                AND COALESCE(u.nickname, '') ILIKE ('%' || :search || '%')
                        )
                    WHEN :searchType = 'PHONE' THEN
                        EXISTS (
                            SELECT 1
                            FROM push_detail pd
                            JOIN users u ON u.id = pd.user_id
                            WHERE pd.push_id = p.id
                                AND COALESCE(u.phone, '') ILIKE ('%' || :search || '%')
                        )
                    ELSE TRUE
                END
            )
        """
    )
    suspend fun countList(
        type: String?,
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
    ): Int

    @Query(
        """
        SELECT p.*
        FROM push p
        WHERE
            (
                :type IS NULL OR :type = 'ALL'
                OR p.code ILIKE ('%' || :type || '%')
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND p.deleted_at IS NULL)
                OR (:status = 'DELETED' AND p.deleted_at IS NOT NULL)
            )
            AND (
                :startDate IS NULL
                OR p.created_at >= :startDate
            )
            AND (
                :endDate IS NULL
                OR p.created_at < :endDate
            )
            AND (
                COALESCE(TRIM(:search), '') = ''
                OR CASE
                    WHEN :searchType = 'ALL' THEN
                        (
                            COALESCE(p.title, '') ILIKE ('%' || :search || '%')
                            OR COALESCE(p.message, '') ILIKE ('%' || :search || '%')
                            OR EXISTS (
                                SELECT 1
                                FROM push_detail pd
                                JOIN users u ON u.id = pd.user_id
                                WHERE pd.push_id = p.id
                                    AND (
                                        COALESCE(u.nickname, '') ILIKE ('%' || :search || '%')
                                        OR COALESCE(u.phone, '') ILIKE ('%' || :search || '%')
                                    )
                            )
                        )
                    WHEN :searchType = 'TITLE' THEN
                        COALESCE(p.title, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'MESSAGE' THEN
                        COALESCE(p.message, '') ILIKE ('%' || :search || '%')
                    WHEN :searchType = 'NICKNAME' THEN
                        EXISTS (
                            SELECT 1
                            FROM push_detail pd
                            JOIN users u ON u.id = pd.user_id
                            WHERE pd.push_id = p.id
                                AND COALESCE(u.nickname, '') ILIKE ('%' || :search || '%')
                        )
                    WHEN :searchType = 'PHONE' THEN
                        EXISTS (
                            SELECT 1
                            FROM push_detail pd
                            JOIN users u ON u.id = pd.user_id
                            WHERE pd.push_id = p.id
                                AND COALESCE(u.phone, '') ILIKE ('%' || :search || '%')
                        )
                    ELSE TRUE
                END
            )
        ORDER BY
            CASE WHEN :order = 'ASC' THEN p.id END ASC,
            CASE WHEN :order = 'DESC' THEN p.id END DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun list(
        type: String?,
        status: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        searchType: String?,
        search: String?,
        order: String,
        limit: Int,
        offset: Int,
    ): List<PushEntity>

    @Modifying
    @Query("UPDATE push SET deleted_at = now() WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: Long): Int
}

