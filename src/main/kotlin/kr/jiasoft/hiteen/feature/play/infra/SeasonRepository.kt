package kr.jiasoft.hiteen.feature.play.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.play.domain.SeasonEntity
import kr.jiasoft.hiteen.feature.play.dto.SeasonRoundResponse
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface SeasonRepository : CoroutineCrudRepository<SeasonEntity, Long> {

    fun findAllByEndDate(endDate: LocalDate): Flow<SeasonEntity>

    //close
    @Query("UPDATE seasons SET status = 'CLOSED' WHERE id = :seasonId")
    suspend fun close(seasonId: Long)

    @Query("UPDATE seasons SET status = 'CLOSED' WHERE end_date = :date")
    suspend fun closeSeasonsEndingAt(date: LocalDate)

    @Query("SELECT EXISTS(SELECT 1 FROM seasons WHERE start_date = :date)")
    suspend fun existsByStartDate(date: LocalDate): Boolean

    @Query("SELECT * FROM seasons WHERE league = :league ORDER BY start_date desc, season_no DESC LIMIT 1")
    suspend fun findLatestByLeague(league: String): SeasonEntity?

    @Query("SELECT * FROM seasons WHERE status = 'ACTIVE' AND league = :league ORDER BY start_date DESC LIMIT 1")
    suspend fun findActiveSeason(league: String): SeasonEntity?

    @Query("SELECT * FROM seasons WHERE status = 'ACTIVE'")
    fun findActiveSeasons(): Flow<SeasonEntity>


//    ,ROW_NUMBER() OVER (
//    PARTITION BY EXTRACT(YEAR FROM s.start_date), EXTRACT(MONTH FROM s.start_date), s.league
//    ORDER BY s.start_date
//    ) AS round_no
    @Query(
        """
        SELECT s.id,
               s.season_no,
               s.start_date,
               s.end_date,
               s.league,
               s.status,
               EXTRACT(YEAR FROM s.start_date)  AS year,
               EXTRACT(MONTH FROM s.start_date) AS month
        FROM seasons s
        WHERE EXTRACT(YEAR FROM s.start_date) = :year
          AND s.league = :league
          AND (:status IS NULL OR s.status = :status)
        ORDER BY year DESC, month, season_no desc
        """
    )
    fun findSeasonsByYearAndLeagueAndStatus(
        year: Int,
        league: String,
        status: String?
    ): Flow<SeasonRoundResponse>



}