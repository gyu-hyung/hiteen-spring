package kr.jiasoft.hiteen.eloquent.dto

data class CursorResult<T, C>(
    val data: List<T>,
    val nextCursor: C?,
    val hasMore: Boolean
)