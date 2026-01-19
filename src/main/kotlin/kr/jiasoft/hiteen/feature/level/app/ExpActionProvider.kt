package kr.jiasoft.hiteen.feature.level.app

import kr.jiasoft.hiteen.feature.level.domain.ExpActionProperty
import kr.jiasoft.hiteen.feature.level.domain.ExpProperties
import kr.jiasoft.hiteen.feature.level.infra.ExpActionRepository
import org.springframework.stereotype.Component

/**
 * 경험치 액션 정의 제공자
 * - DB(exp_actions) 우선 조회
 * - 전환기 안정성을 위해 exp.yml(ExpProperties) fallback 지원
 */
@Component
class ExpActionProvider(
    private val expActionRepository: ExpActionRepository,
    private val props: ExpProperties,
) {

    suspend fun get(actionCode: String): ExpActionProperty? {
        val db = expActionRepository.findByActionCode(actionCode)
        if (db != null && db.enabled) {
            return ExpActionProperty(
                description = db.description,
                points = db.points,
                dailyLimit = db.dailyLimit,
            )
        }

        return props.actions[actionCode]
    }
}

