package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminReportResponse
import kr.jiasoft.hiteen.feature.report.domain.ReportEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdminReportRepository : CoroutineCrudRepository<ReportEntity, Long> {

    @Query("""
        SELECT
            r.id,
            u.uid AS user_uid,
            u.nickname AS user_nickname,
            t.uid AS target_uid,
            t.nickname AS target_nickname,
            r.type,
            r.reportable_type,
            r.reportable_id,
            r.reason,
            r.photo_uid,
            r.status,
            r.answer,
            r.answer_at,
            r.memo,
            r.created_at,
            r.updated_at,
            r.deleted_at
        FROM reports r
        JOIN users u ON u.id = r.user_id
        LEFT JOIN users t ON t.id = r.target_id
        WHERE
            r.deleted_at IS NULL
            AND (:status IS NULL OR r.status = :status)
            AND (:type IS NULL OR r.type = UPPER(:type))
            AND (:userUid IS NULL OR u.uid = :userUid)
            AND (:targetUid IS NULL OR t.uid = :targetUid)
        ORDER BY
            r.status ASC,
            CASE WHEN :order = 'DESC' THEN r.id END DESC,
            CASE WHEN :order = 'ASC'  THEN r.id END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        status: Int?,
        type: String?,
        userUid: UUID?,
        targetUid: UUID?,
    ): Flow<AdminReportResponse>

    @Query("""
        SELECT COUNT(*)
        FROM reports r
        JOIN users u ON u.id = r.user_id
        LEFT JOIN users t ON t.id = r.target_id
        WHERE
            r.deleted_at IS NULL
            AND (:status IS NULL OR r.status = :status)
            AND (:type IS NULL OR r.type = UPPER(:type))
            AND (:userUid IS NULL OR u.uid = :userUid)
            AND (:targetUid IS NULL OR t.uid = :targetUid)
    """)
    suspend fun totalCount(
        status: Int?,
        type: String?,
        userUid: UUID?,
        targetUid: UUID?,
    ): Int

    @Query("""
        SELECT
            r.id,
            u.uid AS user_uid,
            u.nickname AS user_nickname,
            t.uid AS target_uid,
            t.nickname AS target_nickname,
            r.type,
            r.reportable_type,
            r.reportable_id,
            r.reason,
            r.photo_uid,
            r.status,
            r.answer,
            r.answer_at,
            r.memo,
            r.created_at,
            r.updated_at,
            r.deleted_at
        FROM reports r
        JOIN users u ON u.id = r.user_id
        LEFT JOIN users t ON t.id = r.target_id
        WHERE r.id = :id
        LIMIT 1
    """)
    suspend fun findDetailById(id: Long): AdminReportResponse?
}
