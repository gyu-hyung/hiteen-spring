package kr.jiasoft.hiteen.feature.batch

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.giftishow.app.GiftishowSyncService
import kr.jiasoft.hiteen.feature.play.app.GameManageService
import kr.jiasoft.hiteen.feature.school.app.SchoolFoodImportService
import kr.jiasoft.hiteen.feature.school.app.SchoolImportService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BatchService(
    private val schoolImportService: SchoolImportService,
    private val schoolFoodImportService: SchoolFoodImportService,
    private val gameManageService: GameManageService,
    private val giftishowSyncService: GiftishowSyncService,
    @param:Value("\${batch.enabled:false}")//배치 활성화 여부
    private val active: Boolean
) {

    private val logger = LoggerFactory.getLogger(javaClass)


    /**
     * 매일 새벽 6시에 급식 정보를 가져와 갱신한다.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
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
     * 매월 1일 새벽 1시에 학교 정보를 싱크한다.
     */
    @Scheduled(cron = "0 0 1 1 * *", zone = "Asia/Seoul")
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

    /**
     * 매일 4시에 기프트쇼 정보 싱크
     */
//    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    fun syncGiftishowData() = runBlocking {
        if (!active) {
            logger.info("===== Giftishow Sync Batch SKIPPED (inactive) =====")
            return@runBlocking
        }

        logger.info("===== Giftishow Sync Batch START =====")
        try {
            giftishowSyncService.syncGoods()
            giftishowSyncService.syncBrandsAndCategories()
            logger.info("===== Giftishow Sync Batch SUCCESS =====")
        } catch (e: Exception) {
            logger.error("===== Giftishow Sync Batch ERROR: ${e.message}", e)
        }
        logger.info("===== Giftishow Sync Batch END =====")
    }

}
