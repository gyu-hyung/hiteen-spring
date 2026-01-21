package kr.jiasoft.hiteen.feature.board.infra

import kotlinx.coroutines.flow.Flow
import kr.jiasoft.hiteen.feature.board.domain.BoardBannerEntity
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardBannerRepository : CoroutineCrudRepository<BoardBannerEntity, Long> {

    @Query("SELECT * FROM board_banners WHERE board_id = :boardId ORDER BY banner_type ASC, seq ASC, id ASC")
    fun findAllByBoardIdOrderByTypeAndSeq(boardId: Long): Flow<BoardBannerEntity>

    @Modifying
    @Query("DELETE FROM board_banners WHERE board_id = :boardId")
    suspend fun deleteByBoardId(boardId: Long): Int

    @Modifying
    @Query("""
        DELETE FROM board_banners
        WHERE board_id = :boardId
          AND banner_type = :bannerType
          AND uid = ANY(:uids)
    """)
    suspend fun deleteByBoardIdAndBannerTypeAndUidIn(boardId: Long, bannerType: String, uids: Array<java.util.UUID>): Int
}
