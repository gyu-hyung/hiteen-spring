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

/**
 * 공통 페이지 생성 유틸
 */
object PageUtil {

    fun <T> of(
        items: List<T>,
        total: Int,
        page: Int,
        size: Int
    ): ApiPage<T> {

        val currentPage = if (page <= 0) 1 else page
        val perPage = if (size <= 0) items.size else size

        val lastPage = if (total == 0) 1 else ((total - 1) / perPage) + 1

        return ApiPage(
            total = total,
            lastPage = lastPage,
            items = items,
            perPage = perPage,
            currentPage = currentPage,
        )
    }
}
