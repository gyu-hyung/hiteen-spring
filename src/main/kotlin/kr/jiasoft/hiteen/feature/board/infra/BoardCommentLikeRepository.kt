package kr.jiasoft.hiteen.feature.board.infra

import kr.jiasoft.hiteen.feature.board.domain.BoardCommentLikeEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BoardCommentLikeRepository : CoroutineCrudRepository<BoardCommentLikeEntity, Long> {
    suspend fun findByCommentIdAndUserId(commentId: Long, userId: Long): BoardCommentLikeEntity?
    suspend fun deleteByCommentIdAndUserId(commentId: Long, userId: Long)
    suspend fun countByCommentId(commentId: Long): Long
}