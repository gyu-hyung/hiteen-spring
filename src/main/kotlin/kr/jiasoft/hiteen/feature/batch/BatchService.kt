package kr.jiasoft.hiteen.feature.batch

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.play.app.GameManageService
import kr.jiasoft.hiteen.feature.school.app.SchoolFoodImportService
import kr.jiasoft.hiteen.feature.school.app.SchoolImportService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BatchService(
    private val schoolImportService: SchoolImportService,
    private val schoolFoodImportService: SchoolFoodImportService,
    private val gameManageService: GameManageService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 배치 활성화 여부
     * (false 시 모든 스케줄 실행 방지)
     */
    private val active = false


    /**
     * 매일 새벽 6시에 급식 정보를 가져와 갱신한다.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    fun runDailyImport() = runBlocking {
        if (!active) {
            logger.info("===== SchoolFood Batch SKIPPED (inactive) =====")
            return@runBlocking
        }

        logger.info("===== SchoolFood Batch START =====")
        try {
            schoolFoodImportService.import()
            logger.info("===== SchoolFood Batch SUCCESS =====")
        } catch (e: Exception) {
            logger.error("===== SchoolFood Batch ERROR: ${e.message}", e)
        }
        logger.info("===== SchoolFood Batch END =====")
    }


    /**
     * 매월 1일 새벽 3시에 학교 정보를 싱크한다.
     */
    @Scheduled(cron = "0 0 3 1 * *", zone = "Asia/Seoul")
    fun syncSchoolData() = runBlocking {
        if (!active) {
            logger.info("===== School Import Batch SKIPPED (inactive) =====")
            return@runBlocking
        }

        logger.info("===== School Import Batch START =====")
        try {
            schoolImportService.import()
            logger.info("===== School Import Batch SUCCESS =====")
        } catch (e: Exception) {
            logger.error("===== School Import Batch ERROR: ${e.message}", e)
        }
        logger.info("===== School Import Batch END =====")
    }


    /**
     * 매일 0시에 게임 시즌 종료 처리
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    fun gameSeasonClose() = runBlocking {
        if (!active) {
            logger.info("===== Game Close Batch SKIPPED (inactive) =====")
            return@runBlocking
        }

        logger.info("===== Game Close Batch START =====")
        try {
            gameManageService.autoManageSeasons()
            logger.info("===== Game Close Batch SUCCESS =====")
        } catch (e: Exception) {
            logger.error("===== Game Close Batch ERROR: ${e.message}", e)
        }
        logger.info("===== Game Close Batch END =====")
    }
}
