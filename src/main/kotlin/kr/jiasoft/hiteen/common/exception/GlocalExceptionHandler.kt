package kr.jiasoft.hiteen.common.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException


class BusinessValidationException(
    val errors: Map<String, String>
) : RuntimeException("validation failed")

@RestControllerAdvice
class GlobalExceptionHandler {

    // Bean Validation(@Valid)
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(ex: WebExchangeBindException): ResponseEntity<Map<String, String?>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity.badRequest().body(errors)
    }

    // 서비스단 비즈니스 검증(중복 등) -> 동일 포맷으로 변환
    @ExceptionHandler(BusinessValidationException::class)
    fun handleBusiness(ex: BusinessValidationException)
            : ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(ex.errors)

    // 혹시 DB UNIQUE가 직접 터진 경우(레이스 대비) → 필드명 추출해서 동일 포맷
    @ExceptionHandler(org.springframework.dao.DuplicateKeyException::class)
    fun handleDuplicate(e: org.springframework.dao.DuplicateKeyException)
            : ResponseEntity<Map<String, String>> {
        val msg = e.message.orEmpty()
        val errors = when {
            msg.contains("users_email_key", true) -> mapOf("email" to "이미 사용 중인 이메일입니다.")
            msg.contains("users_username_key", true) -> mapOf("username" to "이미 사용 중인 사용자명입니다.")
            else -> mapOf("value" to "중복 데이터입니다.")
        }
        return ResponseEntity.status(409).body(errors) // 원하면 400으로 맞춰도 됩니다.
    }
}