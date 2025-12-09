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

    fun findAllByEndDateOrderById(endDate: LocalDate): Flow<SeasonEntity>

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


    @Query("""
        SELECT  s.id,
                s.season_no,
                s.year,
                s.month,
                s.round,
                s.start_date,
                s.end_date,
                s.status
        FROM seasons s
        WHERE EXTRACT(YEAR FROM s.start_date) = :year
          AND (:status IS NULL OR s.status = :status)
        ORDER BY s.season_no
    """)
    fun findSeasonsByYearAndStatus(
        year: Int,
        status: String?
    ): Flow<SeasonRoundResponse>



}