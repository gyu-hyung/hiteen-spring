package kr.jiasoft.hiteen.eloquent.dto

data class PageResult<T>(
    val data: List<T>,
    val total: Long,
    val perPage: Int,
    val currentPage: Int,
    val lastPage: Long
)