package kr.jiasoft.hiteen.feature.board.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.domain.BoardAssetEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BoardAssetRepository : CoroutineCrudRepository<BoardAssetEntity, Long> {
    suspend fun findAllByBoardId(boardId: Long): List<BoardAssetEntity>?

    @Query("""
        SELECT *
        FROM board_assets
        WHERE board_id = ANY(:boardIds)
        ORDER BY board_id ASC, id ASC
    """)
    fun findAllByBoardIdIn(boardIds: Array<Long>): Flow<BoardAssetEntity>

    suspend fun deleteByBoardId(boardId: Long)
    suspend fun deleteByBoardIdAndUidIn(boardId: Long, uids: Collection<UUID>): Int
    suspend fun deleteByBoardIdAndUid(boardId: Long, uid: UUID)

    // 남은 자산 중 대표 대체 선택용(가장 최근 추가 = id DESC 기준)
    @Query("""
        SELECT uid
        FROM board_assets
        WHERE board_id = :boardId
        ORDER BY id DESC
        LIMIT 1
    """)
    suspend fun findTopUidByBoardIdOrderByIdDesc(boardId: Long): UUID?

}