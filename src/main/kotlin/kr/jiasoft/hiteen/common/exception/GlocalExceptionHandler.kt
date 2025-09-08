package kr.jiasoft.hiteen.common.exception

import kr.jiasoft.hiteen.common.dto.ApiResult
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException

class BusinessValidationException(
    val errors: Map<String, String>
) : RuntimeException("validation failed")

@RestControllerAdvice
class GlobalExceptionHandler {

    // Bean Validation (@Valid) 에러
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(ex: WebExchangeBindException): ResponseEntity<ApiResult<Nothing>> {
        val errors: Map<String, List<String>> =
            ex.bindingResult.fieldErrors
                .groupBy({ it.field }, { it.defaultMessage ?: "잘못된 값이에요." })

        return ResponseEntity
            .badRequest()
            .body(ApiResult(success = false, errors = errors))
    }

    // 서비스단 비즈니스 검증 (중복 체크 등)
    @ExceptionHandler(BusinessValidationException::class)
    fun handleBusiness(ex: BusinessValidationException): ResponseEntity<ApiResult<Nothing>> {
        val errors: Map<String, List<String>> = ex.errors.mapValues { listOf(it.value) }

        return ResponseEntity
            .badRequest()
            .body(ApiResult(success = false, errors = errors))
    }

    // DB UNIQUE 제약조건 위반 (중복 예외)
    @ExceptionHandler(DuplicateKeyException::class)
    fun handleDuplicate(e: DuplicateKeyException): ResponseEntity<ApiResult<Nothing>> {
        val msg = e.message.orEmpty()
        val errors: Map<String, List<String>> = when {
            msg.contains("users_email_key", true) -> mapOf("email" to listOf("이미 사용 중인 이메일입니다."))
            msg.contains("users_username_key", true) -> mapOf("username" to listOf("이미 사용 중인 사용자명입니다."))
            else -> mapOf("value" to listOf("중복 데이터입니다."))
        }

        return ResponseEntity
            .status(409) // 혹은 400 Bad Request로 맞춰도 됨
            .body(ApiResult(success = false, errors = errors))
    }
}
