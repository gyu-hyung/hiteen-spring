package kr.jiasoft.hiteen.common.dto

/*
    API 기본 응답
 */
data class ApiResult<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errors: Map<String, *>? = null,
    val extras: Map<String, *>? = null
) {
    companion object {
        fun <T> success(data: T? = null, message: String? = null): ApiResult<T> =
            ApiResult(success = true, data = data, message = message)

        fun <T> failure(message: String? = null, errors: Map<String, *>? = null): ApiResult<T> =
            ApiResult(success = false, data = null, message = message, errors = errors)

        fun <T> failure(extras: Map<String, *>? = null): ApiResult<T> =
            ApiResult(success = false, data = null, message = "실패", extras = extras)

        fun <T> failure(error: String): ApiResult<T> =
            ApiResult(success = false, data = null, message = "실패", errors = mapOf("code" to listOf(error)))

    }
}
