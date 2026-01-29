package kr.jiasoft.hiteen.feature.asset.app.event

import kr.jiasoft.hiteen.feature.asset.domain.ThumbnailMode
import java.util.UUID

/**

import java.util.UUID
import kr.jiasoft.hiteen.feature.asset.domain.ThumbnailMode

 * - 현재는 단순 in-memory 이벤트이므로 서버 재시작/크래시 시 유실될 수 있습니다.
 * - 푸시 이벤트와 동일하게, 메인 트랜잭션/응답을 깨면 안 되므로 리스너에서 예외는 삼킵니다.
 * NOTE:
 *
 * 썸네일 사전 생성(사이드이펙트)을 요청-응답 흐름과 분리하기 위한 이벤트.
 */
data class AssetThumbnailPrecreateRequestedEvent(
    val requestedByUserId: Long? = null,
    val mode: ThumbnailMode = ThumbnailMode.COVER,
    val height: Int,
    val width: Int,
    val assetUids: List<UUID>,
)