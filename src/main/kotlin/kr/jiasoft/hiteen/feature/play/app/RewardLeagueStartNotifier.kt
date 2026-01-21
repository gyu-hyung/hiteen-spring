package kr.jiasoft.hiteen.feature.play.app

import kr.jiasoft.hiteen.feature.play.domain.RewardLeagueStartNotificationEntity
import kr.jiasoft.hiteen.feature.play.infra.RewardLeagueStartNotificationRepository
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.user.domain.PushItemType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

/**
 * (시즌, 리그, 게임) 단위 점수 등록자 수가 10명 이상이 되는 순간
 * '보상 리그 시작' 알림을 1회만 발송하기 위한 서비스.
 *
 * - 중복 발송 방지는 reward_league_start_notifications 테이블의 UNIQUE 제약으로 보장
 */
@Service
class RewardLeagueStartNotifier(
    private val repo: RewardLeagueStartNotificationRepository,
    private val pushService: PushService,
) {

    suspend fun notifyIfReached(
        seasonId: Long,
        league: String,
        gameId: Long,
        threshold: Long = 10,
    ) {
        // 이미 발송한 케이스면 빠르게 종료
        if (repo.existsBySeasonIdAndLeagueAndGameId(seasonId, league, gameId)) return

        val count = repo.countScoreParticipants(seasonId, league, gameId)
        if (count < threshold) return

        // ✅ 여기서부터는 '발송 시도'
        // 동시성 상황에서도 insert 성공한 1개만 실제 발송
        val inserted = try {
            repo.save(
                RewardLeagueStartNotificationEntity(
                    seasonId = seasonId,
                    league = league,
                    gameId = gameId,
                )
            )
            true
        } catch (_: DataIntegrityViolationException) {
            false
        }

        if (!inserted) return

        // 알림 대상: FCM Topic 기반(전체 발송)
        // - 실제 발송은 토픽 1회
        // - push/push_detail 기록은 PushService에서 user_details(토큰) 기준으로 생성
        val topic = PushTemplate.REWARD_LEAGUE_START.itemType ?: PushItemType.ALL

        pushService.sendAndSavePushToTopic(
            topic = topic,
            userId = null,
            templateData = PushTemplate.REWARD_LEAGUE_START.buildPushData(
                "league" to league,
            ),
            extraData = mapOf(
                "seasonId" to seasonId.toString(),
                "league" to league,
                "gameId" to gameId.toString(),
            )
        )
    }
}
