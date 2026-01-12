package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.AdminRankingResponse
import kr.jiasoft.hiteen.feature.play.domain.GameRankingEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AdminRankingListRepository : CoroutineCrudRepository<GameRankingEntity, Long>{

    /**
     * 실시간 랭킹 (game_scores) 목록 조회
     */
    @Query("""
        SELECT
            'REALTIME' AS source,
            ROW_NUMBER() OVER (
                ORDER BY gs.score ASC,
                         COALESCE(gs.updated_at, gs.created_at) ASC
            ) AS rank,
            gs.season_id AS season_id,
            gs.game_id AS game_id,
            sp.league AS league,
            u.id AS user_id,
            u.uid AS user_uid,
            u.nickname AS nickname,
            gs.score AS score,
            gs.try_count AS try_count,
            COALESCE(gs.updated_at, gs.created_at) AS created_at
        FROM game_scores gs
        JOIN season_participants sp ON gs.participant_id = sp.id
        JOIN users u ON sp.user_id = u.id
        WHERE
            (:seasonId IS NULL OR gs.season_id = :seasonId)
          AND (:gameId IS NULL OR gs.game_id = :gameId)
          AND (:league IS NULL OR :league = 'ALL' OR sp.league = :league)
          AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND gs.deleted_at IS NULL)
                OR (:status = 'DELETED' AND gs.deleted_at IS NOT NULL)
          )
          AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        u.nickname ILIKE CONCAT('%', :search, '%')
                        OR CAST(u.id AS TEXT) ILIKE CONCAT('%', :search, '%')
                        OR CAST(u.uid AS TEXT) ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'userId' AND CAST(u.id AS TEXT) ILIKE CONCAT('%', :search, '%'))
          )
          AND (
                :uid IS NULL OR u.uid = :uid
          )
        ORDER BY
            CASE WHEN :order = 'DESC' THEN gs.score END DESC,
            CASE WHEN :order = 'ASC' THEN gs.score END ASC,
            CASE WHEN :order = 'DESC' THEN COALESCE(gs.updated_at, gs.created_at) END DESC,
            CASE WHEN :order = 'ASC' THEN COALESCE(gs.updated_at, gs.created_at) END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun realtimeListByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        seasonId: Long?,
        gameId: Long?,
        league: String?,
    ): Flow<AdminRankingResponse>

    @Query("""
        SELECT COUNT(*)
        FROM game_scores gs
        JOIN season_participants sp ON gs.participant_id = sp.id
        JOIN users u ON sp.user_id = u.id
        WHERE
            (:seasonId IS NULL OR gs.season_id = :seasonId)
          AND (:gameId IS NULL OR gs.game_id = :gameId)
          AND (:league IS NULL OR :league = 'ALL' OR sp.league = :league)
          AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = 'ACTIVE' AND gs.deleted_at IS NULL)
                OR (:status = 'DELETED' AND gs.deleted_at IS NOT NULL)
          )
          AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        u.nickname ILIKE CONCAT('%', :search, '%')
                        OR CAST(u.id AS TEXT) ILIKE CONCAT('%', :search, '%')
                        OR CAST(u.uid AS TEXT) ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'nickname' AND u.nickname ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'userId' AND CAST(u.id AS TEXT) ILIKE CONCAT('%', :search, '%'))
          )
          AND (
                :uid IS NULL OR u.uid = :uid
          )
    """)
    suspend fun realtimeTotalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        seasonId: Long?,
        gameId: Long?,
        league: String?,
    ): Int


    /**
     * 시즌 저장 랭킹 (game_rankings) 목록 조회
     */
    @Query("""
        SELECT
            'SEASON' AS source,
            gr.rank::bigint AS rank,
            gr.season_id AS season_id,
            CONCAT(
                RIGHT(CAST(s.year AS VARCHAR), 2), '-',
                LPAD(month::text, 2, '0'), '-',
                s.round
            ) AS season_no,
            gr.game_id AS game_id,
            (select name from games where id = gr.game_id) AS game_name,
            gr.league AS league,
            gr.user_id AS user_id,
            u.uid AS user_uid,
            gr.nickname AS nickname,
            gr.score AS score,
            NULL::int AS try_count,
            gr.created_at AS created_at
        FROM game_rankings gr
        LEFT JOIN users u ON gr.user_id = u.id
        LEFT JOIN seasons s ON s.id = gr.season_id 
        WHERE (:seasonId IS NULL OR gr.season_id = :seasonId)
          AND (:gameId IS NULL OR gr.game_id = :gameId)
          AND (:league IS NULL OR :league = 'ALL' OR gr.league = :league)
          AND (
                :status IS NULL OR :status = 'ALL'
          )
          AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        gr.nickname ILIKE CONCAT('%', :search, '%')
                        OR CAST(gr.user_id AS TEXT) ILIKE CONCAT('%', :search, '%')
                        OR CAST(u.uid AS TEXT) ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'nickname' AND gr.nickname ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'userId' AND CAST(gr.user_id AS TEXT) ILIKE CONCAT('%', :search, '%'))
          )
          AND (
                :uid IS NULL OR u.uid = :uid
          )
        ORDER BY
            CASE WHEN :order = 'DESC' THEN gr.season_id END DESC,
            CASE WHEN :order = 'ASC' THEN gr.season_id END ASC,
            CASE WHEN :order = 'DESC' THEN gr.rank END DESC,
            CASE WHEN :order = 'ASC' THEN gr.rank END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun seasonListByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        seasonId: Long?,
        gameId: Long?,
        league: String?,
    ): Flow<AdminRankingResponse>

    @Query("""
        SELECT COUNT(*)
        FROM game_rankings gr
        LEFT JOIN users u ON gr.user_id = u.id
        LEFT JOIN seasons s ON s.id = gr.season_id 
        WHERE
            (:seasonId IS NULL OR gr.season_id = :seasonId)
          AND (:gameId IS NULL OR gr.game_id = :gameId)
          AND (:league IS NULL OR :league = 'ALL' OR gr.league = :league)
          AND (
                :status IS NULL OR :status = 'ALL'
          )
          AND (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        gr.nickname ILIKE CONCAT('%', :search, '%')
                        OR CAST(gr.user_id AS TEXT) ILIKE CONCAT('%', :search, '%')
                        OR CAST(u.uid AS TEXT) ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'nickname' AND gr.nickname ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'userId' AND CAST(gr.user_id AS TEXT) ILIKE CONCAT('%', :search, '%'))
          )
          AND (
                :uid IS NULL OR u.uid = :uid
          )
    """)
    suspend fun seasonTotalCount(
        search: String?,
        searchType: String,
        status: String?,
        uid: UUID?,
        seasonId: Long?,
        gameId: Long?,
        league: String?,
    ): Int
}

