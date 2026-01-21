package kr.jiasoft.hiteen.feature.board.app.event

import java.util.UUID

/**
 * 게시글 생성 후(커밋 이후) 처리해야 하는 사이드 이펙트를 위한 이벤트.
 *
 * payload는 가볍게 유지하고(필요하면 listener에서 다시 조회),
 * 트랜잭션 커밋 이후에 푸시 발송을 비동기로 수행하는 용도.
 */
data class BoardCreatedEvent(
    val boardUid: UUID,
    val authorId: Long,
)

