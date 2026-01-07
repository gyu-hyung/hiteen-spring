package kr.jiasoft.hiteen.common.helpers

import kr.jiasoft.hiteen.common.dto.ApiResult
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object ApiResponseHelper {
    private val log = LoggerFactory.getLogger(ApiResponseHelper::class.java)

    /* =========================
     * SUCCESS
     * =========================
     */
    fun <T> success(
        data: T? = null,
        message: String? = null,
        status: HttpStatus = HttpStatus.OK
    ): ResponseEntity<ApiResult<T>> {

        val result = ApiResult.success(data, message)
        logDebug(result)

        return ResponseEntity.status(status).body(result)
    }

    /* =========================
     * FAILURE
     * =========================
     */
    fun <T> failure(
        message: String? = null,
        errors: Map<String, *>? = null,
        status: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY
    ): ResponseEntity<ApiResult<T>> {

        val result = ApiResult.failure<T>(message, errors)
        logDebug(result)

        return ResponseEntity.status(status).body(result)
    }

    fun <T> failure(
        extras: Map<String, *>?,
        status: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY
    ): ResponseEntity<ApiResult<T>> {

        val result = ApiResult.failure<T>(extras)
        logDebug(result)

        return ResponseEntity.status(status).body(result)
    }

    fun <T> failure(
        error: String,
        status: HttpStatus = HttpStatus.UNPROCESSABLE_ENTITY
    ): ResponseEntity<ApiResult<T>> {

        val result = ApiResult.failure<T>(error)
        logDebug(result)

        return ResponseEntity.status(status).body(result)
    }

    /* =========================
     * LOG
     * ========================= */
    private fun logDebug(any: Any) =
        runCatching {
            log.debug(any.toString())
        }.onFailure {
            log.debug(it.message)
        }
}