package kr.jiasoft.hiteen.feature.poll.infra

import kr.jiasoft.hiteen.feature.poll.domain.PollCommentLikeEntity
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PollCommentLikeRepository : CoroutineCrudRepository<PollCommentLikeEntity, Long>{
    suspend fun deleteByCommentIdAndUserId(boardId: Long, userId: Long)
}