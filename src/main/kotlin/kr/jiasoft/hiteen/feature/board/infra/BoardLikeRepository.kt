package kr.jiasoft.hiteen.feature.board.infra

import kr.jiasoft.hiteen.feature.board.domain.BoardLikeEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardLikeRepository : CoroutineCrudRepository<BoardLikeEntity, Long> {
    suspend fun findByBoardIdAndUserId(boardId: Long, userId: Long): BoardLikeEntity?
    suspend fun deleteByBoardIdAndUserId(boardId: Long, userId: Long)
    suspend fun countByBoardId(boardId: Long): Long
}