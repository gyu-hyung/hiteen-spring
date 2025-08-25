package kr.jiasoft.hiteen.common.dto

/*
    커서 기반 페이지네이션
 */
data class ApiPageCursor<T>(
    val nextCursor: String?,   // 다음 페이지 커서 (없으면 null)
    val items: List<T>,        // 데이터 목록
    val perPage: Int           // 페이지당 개수
)
