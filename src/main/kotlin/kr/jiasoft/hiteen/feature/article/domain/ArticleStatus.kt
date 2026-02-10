package kr.jiasoft.hiteen.feature.article.domain

/**
 * Article 상태
 * - ACTIVE: 활성 (진행중)
 * - INACTIVE: 비활성
 * - ENDED: 종료됨
 * - WINNING: 당첨자 발표
 */
enum class ArticleStatus {
    ACTIVE,             // 진행중 (공지사항/이벤트)
    INACTIVE,           // 비활성
    ENDED,              // 종료됨 (이벤트)
    WINNING,            // 당첨자 발표 (이벤트)
}

