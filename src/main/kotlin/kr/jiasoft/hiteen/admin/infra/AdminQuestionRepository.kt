package kr.jiasoft.hiteen.admin.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.admin.dto.SeasonFilterDto
import kr.jiasoft.hiteen.feature.study.domain.QuestionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface AdminQuestionRepository : CoroutineCrudRepository<QuestionEntity, Long> {

    @Query("""
        SELECT
            id AS key,
            CONCAT(
                RIGHT(CAST(year AS VARCHAR), 2), '-',
                LPAD(month::text, 2, '0'), '-',
                round,
                ' 회차'
            ) AS value
        FROM seasons
        WHERE
            (:status IS NULL OR :status = 'ALL')
            OR (:status = 'ACTIVE' AND status = 'ACTIVE')
            OR (:status = 'CLOSED' AND status = 'CLOSED')
        ORDER BY
            year DESC,
            month DESC,
            round DESC
    """)
    fun findSeasonFilters(status: String?): Flow<SeasonFilterDto>


//    @Query("""
//        SELECT
//            id AS key,
//            name AS value
//        FROM games
//        WHERE
//            deleted_at IS NULL
//        ORDER BY
//            created_at DESC,
//            id DESC;
//    """)
    @Query("""
        SELECT
            id AS key,
            name AS value
        FROM games
        WHERE
            deleted_at IS NULL
        ORDER BY id ASC;
    """)
    fun findGameFilters(): Flow<SeasonFilterDto>


    /**
     * 🔹 전체 개수 조회
     */
    @Query("""
        SELECT COUNT(*)
        FROM question_2 q
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        q.question ILIKE CONCAT('%', :search, '%')
                        OR q.answer ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'question' AND q.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'answer' AND q.answer ILIKE CONCAT('%', :search, '%'))
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = '1' AND q.status = 1)
                OR (:status = '0' AND q.status = 0)
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR (:type = '1' AND q.type = 1)
                OR (:type = '2' AND q.type = 2)
                OR (:type = '3' AND q.type = 3)
            )
            AND (
                :seasonId IS NULL OR (exists (select 1 from question_items qi where q.id = qi.question_id and season_id = :seasonId))
            )
            AND (
                :hasAsset IS NULL
                OR (:hasAsset = true AND ((q.image IS NOT NULL AND q.image <> '') OR (q.sound IS NOT NULL AND q.sound <> '')))
                OR (:hasAsset = false AND ((q.image IS NULL OR q.image = '') OR (q.sound IS NULL OR q.sound = '')))
            )
    """)
    suspend fun totalCount(
        search: String?,
        searchType: String,
        status: String?,
        type: String?,
        seasonId: Long?,
        hasAsset: Boolean?,
    ): Int




    /**
     * 🔹 페이징 조회 (AdminFriendResponse)
     */
    @Query("""
        SELECT q.*
        FROM question_2 q
        WHERE
            (
                :search IS NULL
                OR (
                    :searchType = 'ALL' AND (
                        q.question ILIKE CONCAT('%', :search, '%')
                        OR q.answer ILIKE CONCAT('%', :search, '%')
                    )
                )
                OR (:searchType = 'question' AND q.question ILIKE CONCAT('%', :search, '%'))
                OR (:searchType = 'answer' AND q.answer ILIKE CONCAT('%', :search, '%'))
            )
            AND (
                :status IS NULL OR :status = 'ALL'
                OR (:status = '1' AND q.status = 1)
                OR (:status = '0' AND q.status = 0)
            )
            AND (
                :type IS NULL OR :type = 'ALL'
                OR (:type = '1' AND q.type = 1)
                OR (:type = '2' AND q.type = 2)
                OR (:type = '3' AND q.type = 3)
            )
            AND (
                :seasonId IS NULL OR (exists (select 1 from question_items qi where q.id = qi.question_id and season_id = :seasonId))
            )
            AND (
                :hasAsset IS NULL
                OR (:hasAsset = true AND ((q.image IS NOT NULL AND q.image <> '') OR (q.sound IS NOT NULL AND q.sound <> '')))
                OR (:hasAsset = false AND ((q.image IS NULL OR q.image = '') OR (q.sound IS NULL OR q.sound = '')))
            )
        ORDER BY
            q.status DESC,
            CASE WHEN :order = 'DESC' THEN q.created_at END DESC,
            CASE WHEN :order = 'ASC' THEN q.created_at END ASC
        LIMIT :size OFFSET (:page - 1) * :size
    """)
    fun listByPage(
        page: Int,
        size: Int,
        order: String,
        search: String?,
        searchType: String,
        status: String?,
        type: String?,
        seasonId: Long?,
        hasAsset: Boolean?,
    ): Flow<QuestionEntity>



}