package kr.jiasoft.hiteen.common.dto

/*
    숫자 기반 페이지네이션
 */
data class ApiPage<T>(
    val total: Int,
    val lastPage: Int,
    val items: List<T>,
    val perPage: Int,
    val currentPage: Int,
)