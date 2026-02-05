package kr.jiasoft.hiteen.feature.poll.domain

/**
 * 투표 목록 정렬 타입
 *
 * - LATEST: 최신순(기본)
 * - POPULAR: 인기순(투표수)
 * - LIKE: 좋아요순
 * - COMMENT: 댓글순
 * - DISTANCE: 거리순
 */
enum class PollOrderType {
    LATEST,
    POPULAR,
    LIKE,
    COMMENT,
    DISTANCE;

    companion object {
        fun from(value: String?): PollOrderType {
            if (value.isNullOrBlank()) return LATEST
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LATEST
        }
    }
}

