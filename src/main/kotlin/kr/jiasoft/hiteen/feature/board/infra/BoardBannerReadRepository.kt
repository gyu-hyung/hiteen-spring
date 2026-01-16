package kr.jiasoft.hiteen.feature.board.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.domain.BoardBannerEntity
import kr.jiasoft.hiteen.feature.board.dto.BannerRow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 사용자용(event) 배너 분리 조회를 위한 읽기 전용 projection.
 */
@Repository
interface BoardBannerReadRepository : CoroutineCrudRepository<BoardBannerEntity, Long> {

    @Query(
        """
        SELECT board_id AS boardId, uid, banner_type AS bannerType, seq
        FROM board_banners
        WHERE board_id = ANY(:boardIds)
        ORDER BY board_id ASC, banner_type ASC, seq ASC, id ASC
        """
    )
    fun findAllByBoardIdIn(boardIds: Array<Long>): Flow<BannerRow>
}

