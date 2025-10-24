package kr.jiasoft.hiteen.feature.board.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.domain.BoardEntity
import kr.jiasoft.hiteen.feature.board.dto.BoardResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BoardRepository : CoroutineCrudRepository<BoardEntity, Long> {
    suspend fun findByUid(uid: UUID): BoardEntity?
    fun findAllByCategoryAndDeletedAtIsNullOrderByIdDesc(category: String): Flow<BoardEntity>

    @Query("SELECT id FROM boards WHERE uid = :uid")
    suspend fun findIdByUid(uid: UUID): Long?

    @Query("UPDATE boards SET hits = hits + 1 WHERE id = :id")
    suspend fun increaseHits(id: Long)

    suspend fun countByCreatedId(id: Long): Int


    @Query("""
        SELECT COUNT(*)::int
        FROM boards b
        WHERE b.deleted_at IS NULL
          AND (
                (:category IS NULL AND b.category != 'NOTICE')
                OR b.category = :category
          )
          AND (
                :q IS NULL 
                OR :q = '' 
                OR b.subject ILIKE '%' || :q || '%' 
                OR b.content ILIKE '%' || :q || '%'
          )
          AND (
              :followOnly = false OR EXISTS (
                  SELECT 1 FROM follows f WHERE f.user_id = :userId AND f.follow_id = b.created_id
              )
          )
          AND (
              :friendOnly = false OR EXISTS (
                  SELECT 1 FROM friends f 
                  WHERE (f.user_id = :userId AND f.friend_id = b.created_id)
                     OR (f.friend_id = :userId AND f.user_id = b.created_id)
              )
          )
          AND (
              :sameSchoolOnly = false OR EXISTS (
                  SELECT 1 FROM users u 
                  WHERE u.id = b.created_id 
                    AND u.school_id = (SELECT school_id FROM users WHERE id = :userId)
              )
          )
    """)
    suspend fun countSearchResults(
        category: String?,
        q: String?,
        userId: Long,
        followOnly: Boolean,
        friendOnly: Boolean,
        sameSchoolOnly: Boolean
    ): Int



    @Query("""
        SELECT 
            b.id,
            b.uid,
            b.category,
            b.subject,
            b.content,
            b.link,
            b.hits,
            b.asset_uid      AS asset_uid,
            b.start_date,
            b.end_date,
            b.status,
            b.address,
            b.detail_address,
            b.created_at     AS created_at,
            b.created_id     AS created_id,
            (SELECT COUNT(*)::bigint FROM board_likes bl WHERE bl.board_id = b.id) AS like_count,
            (SELECT COUNT(*)::bigint FROM board_comments bc WHERE bc.board_id = b.id AND bc.deleted_at IS NULL) AS comment_count,
            EXISTS (SELECT 1 FROM board_likes bl2 WHERE bl2.board_id = b.id AND bl2.user_id = :userId) AS liked_by_me,
            COALESCE((
                SELECT array_agg(ba.uid) FROM board_assets ba WHERE ba.board_id = b.id
            ), ARRAY[]::uuid[]) AS attachments
        FROM boards b
        WHERE b.deleted_at IS NULL
          AND (
                (:category IS NULL AND b.category != 'NOTICE')
                OR b.category = :category
          )
          AND (
                :q IS NULL 
                OR :q = '' 
                OR b.subject ILIKE '%' || :q || '%' 
                OR b.content ILIKE '%' || :q || '%'
          )
          AND (
              :followOnly = false OR EXISTS (
                  SELECT 1 FROM follows f WHERE f.user_id = :userId AND f.follow_id = b.created_id
              )
          )
          AND (
              :friendOnly = false OR EXISTS (
                  SELECT 1 FROM friends f 
                  WHERE (f.user_id = :userId AND f.friend_id = b.created_id)
                     OR (f.friend_id = :userId AND f.user_id = b.created_id)
              )
          )
          AND (
              :sameSchoolOnly = false OR EXISTS (
                  SELECT 1 FROM users u 
                  WHERE u.id = b.created_id 
                    AND u.school_id = (SELECT school_id FROM users WHERE id = :userId)
              )
          )
        ORDER BY b.id DESC
        LIMIT :limit OFFSET :offset
    """)
    fun searchSummariesByPage(
        category: String?,
        q: String?,
        limit: Int,
        offset: Int,
        userId: Long,
        followOnly: Boolean,
        friendOnly: Boolean,
        sameSchoolOnly: Boolean
    ): Flow<BoardResponse>



    @Query("""
        SELECT 
            b.id,
            b.uid,
            b.category,
            b.subject,
            b.content,
            b.link,
            b.hits,
            b.status,
            b.address,
            b.detail_address,
            b.start_date,
            b.end_date,
            b.asset_uid      AS asset_uid,
            b.created_at     AS created_at,
            b.created_id     AS created_id,
            (SELECT COUNT(*)::bigint FROM board_likes bl WHERE bl.board_id = b.id) AS like_count,
            (SELECT COUNT(*)::bigint FROM board_comments bc WHERE bc.board_id = b.id AND bc.deleted_at IS NULL) AS comment_count,
            EXISTS (SELECT 1 FROM board_likes bl2 WHERE bl2.board_id = b.id AND bl2.user_id = :userId) AS liked_by_me,
            COALESCE((
                SELECT array_agg(ba.uid) FROM board_assets ba WHERE ba.board_id = b.id
            ), ARRAY[]::uuid[]) AS attachments
        FROM boards b
        WHERE b.deleted_at IS NULL
          AND (
                (:category IS NULL AND b.category != 'NOTICE')
                OR b.category = :category
          )
          AND (
                :q IS NULL 
                OR :q = '' 
                OR b.subject ILIKE '%' || :q || '%' 
                OR b.content ILIKE '%' || :q || '%'
          )
          AND (:followOnly = false OR b.created_id IN (
                SELECT f.follow_id FROM follows f WHERE f.user_id = :userId
          ) OR b.created_id = :userId)
          AND (:friendOnly = false OR b.created_id IN (
                SELECT CASE 
                         WHEN f.user_id = :userId THEN f.friend_id 
                         ELSE f.user_id 
                       END
                FROM friends f
                WHERE f.user_id = :userId OR f.friend_id = :userId
          ) OR b.created_id = :userId)
          AND (:sameSchoolOnly = false OR b.created_id IN (
                SELECT u.id FROM users u 
                WHERE u.school_id = (SELECT school_id FROM users WHERE id = :userId)
          ) OR b.created_id = :userId)
          AND (:lastUid IS NULL OR b.id <= (SELECT id FROM boards WHERE uid = :lastUid))
          AND (:authorUid IS NULL OR b.created_id = (SELECT id FROM users WHERE uid = :authorUid))
        ORDER BY b.id DESC
        LIMIT :limit
    """)
    fun searchSummariesByCursor(
        category: String?,
        q: String?,
        limit: Int,
        userId: Long,
        followOnly: Boolean,
        friendOnly: Boolean,
        sameSchoolOnly: Boolean,
        lastUid: UUID?,
        authorUid: UUID?
    ): Flow<BoardResponse>


    @Query("""
        SELECT 
            b.id,
            b.uid,
            b.category,
            b.subject,
            b.content,
            b.link,
            b.hits,
            b.asset_uid      AS asset_uid,
            b.start_date,
            b.end_date,
            b.status,
            b.address,
            b.detail_address,
            b.created_at     AS created_at,
            b.created_id     AS created_id,
            b.updated_at     AS updated_at,
            (SELECT COUNT(*)::bigint FROM board_likes bl WHERE bl.board_id = b.id) AS like_count,
            (SELECT COUNT(*)::bigint FROM board_comments bc WHERE bc.board_id = b.id AND bc.deleted_at IS NULL) AS comment_count,
            EXISTS (SELECT 1 FROM board_likes bl2 WHERE bl2.board_id = b.id AND bl2.user_id = :userId) AS liked_by_me,
            COALESCE((
                SELECT array_agg(ba.uid) FROM board_assets ba WHERE ba.board_id = b.id
            ), ARRAY[]::uuid[]) AS attachments
        FROM boards b
        WHERE b.uid = :uid
          AND b.deleted_at IS NULL
    """)
    suspend fun findDetailByUid(uid: UUID, userId: Long): BoardResponse?



}