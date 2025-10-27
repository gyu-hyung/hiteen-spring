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

    @Query("SELECT * FROM seasons WHERE status = 'ACTIVE' ORDER BY start_date DESC, id DESC LIMIT 1")
    suspend fun findActiveSeason(): SeasonEntity?

    @Query("SELECT * FROM seasons WHERE status = 'ACTIVE'")
    fun findActiveSeasons(): Flow<SeasonEntity>


//    ,ROW_NUMBER() OVER (
//    PARTITION BY EXTRACT(YEAR FROM s.start_date), EXTRACT(MONTH FROM s.start_date), s.league
//    ORDER BY s.start_date
//    ) AS round_no
    @Query("""
        SELECT DISTINCT s.id,
                        s.season_no,
                        s.start_date,
                        s.end_date,
                        s.status,
                        EXTRACT(YEAR FROM s.start_date)  AS year,
                        EXTRACT(MONTH FROM s.start_date) AS month,
                        sp.league
        FROM seasons s
        LEFT JOIN season_participants sp ON sp.season_id = s.id
        WHERE EXTRACT(YEAR FROM s.start_date) = :year
          AND (:status IS NULL OR s.status = :status)
        ORDER BY year DESC, month, s.season_no DESC
    """)
    fun findSeasonsByYearAndLeagueAndStatus(
        year: Int,
        status: String?
    ): Flow<SeasonRoundResponse>



}