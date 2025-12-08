package kr.jiasoft.hiteen.feature.batch

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.batch.domain.BatchHistoryEntity
import kr.jiasoft.hiteen.feature.batch.infra.BatchHistoryRepository
import kr.jiasoft.hiteen.feature.giftishow.app.GiftishowSyncService
import kr.jiasoft.hiteen.feature.play.app.GameManageService
import kr.jiasoft.hiteen.feature.school.app.SchoolFoodImportService
import kr.jiasoft.hiteen.feature.school.app.SchoolImportService
import kr.jiasoft.hiteen.feature.timetable.app.TimeTableImportService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime

@Component
class BatchService(
    private val schoolImportService: SchoolImportService,
    private val schoolFoodImportService: SchoolFoodImportService,
    private val gameManageService: GameManageService,
    private val giftishowSyncService: GiftishowSyncService,
    private val timeTableImportService: TimeTableImportService,

    private val batchHistoryRepository: BatchHistoryRepository,
    @param:Value("\${batch.enabled:false}") // 배치 활성화 여부
    private val active: Boolean
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 공통 배치 실행 헬퍼
     */
    private suspend fun runBatch(jobName: String, block: suspend () -> Unit) {
        // 비활성화 시 SKIPPED 기록
        if (!active) {
            logger.info("===== $jobName Batch SKIPPED (inactive) =====")
            val now = OffsetDateTime.now()
            batchHistoryRepository.save(
                BatchHistoryEntity(
                    jobName = jobName,
                    status = "SKIPPED",
                    startedAt = now,
                    finishedAt = now,
                    durationMs = 0L,
                    errorMessage = "Batch inactive"
                )
            )
            return
        }

        logger.info("===== $jobName Batch START =====")
        val start = OffsetDateTime.now()

        // STARTED 기록
        var history = batchHistoryRepository.save(
            BatchHistoryEntity(
                jobName = jobName,
                status = "STARTED",
                startedAt = start
            )
        )

        try {
            block.invoke()

            val end = OffsetDateTime.now()
            val durationMs = Duration.between(start, end).toMillis()

            history = history.copy(
                status = "SUCCESS",
                finishedAt = end,
                durationMs = durationMs,
                errorMessage = null
            )
            batchHistoryRepository.save(history)

            logger.info("===== $jobName Batch SUCCESS (duration=${durationMs}ms) =====")
        } catch (e: Exception) {
            val end = OffsetDateTime.now()
            val durationMs = Duration.between(start, end).toMillis()

            history = history.copy(
                status = "FAILED",
                finishedAt = end,
                durationMs = durationMs,
                errorMessage = e.message
            )
            batchHistoryRepository.save(history)

            logger.error("===== $jobName Batch ERROR: ${e.message} =====", e)
        }

        logger.info("===== $jobName Batch END =====")
    }


    /**
     * 매월 1일 새벽 1시에 학교, 학급 정보를 싱크한다.
     */
    @Scheduled(cron = "0 0 1  * *", zone = "Asia/Seoul")
    fun schoolImport() = runBlocking {
        runBatch("SchoolImport") {
            schoolImportService.import()
        }
    }


    /**
     * 매일 새벽 2시에 급식 정보를 가져와 갱신한다.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun schoolFoodImport() = runBlocking {
        runBatch("SchoolFoodImport") {
            schoolFoodImportService.import()
        }
    }

    /**
     * 매일 새벽 3시에 시간표 정보를 가져옵니다.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    fun timeTableImport() = runBlocking {
        runBatch("timeTableImport") {
            timeTableImportService.import()
        }
    }

    /**
     * 매일 0시에 게임 시즌 종료 처리
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun autoManageSeasons() = runBlocking {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        runBatch("GameSeasonClose") {
            gameManageService.closeSeasons(yesterday)   // 1. 시즌 종료 및 랭킹 저장
        }
        runBatch("GameSeasonCreate") {
            gameManageService.createNewSeasons(today)   // 2. 새로운 시즌 생성
        }
    }

    /**
     * 매일 4시에 기프트쇼 정보 싱크
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun syncGiftishowData() = runBlocking {
        runBatch("GiftishowSync") {
            giftishowSyncService.syncGoods()
            giftishowSyncService.syncBrandsAndCategories()
        }
    }
}
