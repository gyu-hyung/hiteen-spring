package kr.jiasoft.hiteen.feature.board.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kr.jiasoft.hiteen.feature.board.dto.BoardSummaryRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BoardRepository : CoroutineCrudRepository<BoardEntity, Long> {
    suspend fun findByUid(uid: UUID): BoardEntity?
    fun findAllByCategoryAndDeletedAtIsNullOrderByIdDesc(category: String): Flow<BoardEntity>

    @Query("""
        UPDATE boards SET hits = hits + 1 WHERE id = :id
    """)
    suspend fun increaseHits(id: Long): Int

    @Query("""
        SELECT 
            b.uid,
            b.category,
            b.subject,
            b.content,
            b.link,
            b.hits,
            b.asset_uid      AS asset_uid,
            b.created_at     AS created_at,
            b.created_id     AS created_id,
            (SELECT COUNT(*)::bigint FROM board_likes bl 
               WHERE bl.board_id = b.id)                                       AS like_count,
            (SELECT COUNT(*)::bigint FROM board_comments bc 
               WHERE bc.board_id = b.id AND bc.deleted_at IS NULL)             AS comment_count,
            EXISTS (SELECT 1 FROM board_likes bl2 
                      WHERE bl2.board_id = b.id AND bl2.user_id = :userId)     AS liked_by_me
        FROM boards b
        WHERE b.deleted_at IS NULL
          AND (:category IS NULL OR b.category = :category)
          AND (
                :q IS NULL 
                OR :q = '' 
                OR b.subject ILIKE '%' || :q || '%' 
                OR b.content ILIKE '%' || :q || '%'
          )
        ORDER BY b.id DESC
        LIMIT :limit OFFSET :offset
    """)
    fun searchSummaries(
        category: String?,
        q: String?,
        limit: Int,
        offset: Int,
        userId: Long
    ): Flow<BoardSummaryRow>

}