package kr.jiasoft.hiteen.common.extensions

import kr.jiasoft.hiteen.common.dto.ApiResult
import kr.jiasoft.hiteen.common.helpers.ApiResponseHelper
import org.springframework.http.ResponseEntity

fun <T> success(
    data: T? = null,
    message: String? = null
): ResponseEntity<ApiResult<T>> =
    ApiResponseHelper.success(data, message)

fun <T> failure(
    message: String
): ResponseEntity<ApiResult<T>> =
    ApiResponseHelper.failure(message)
