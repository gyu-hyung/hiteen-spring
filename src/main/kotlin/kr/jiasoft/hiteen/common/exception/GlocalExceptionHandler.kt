package kr.jiasoft.hiteen.common.exception

import kr.jiasoft.hiteen.common.dto.ApiResult
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException


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
            .body(ApiResult.failure(errors))
    }

    // 서비스단 비즈니스 검증 (중복 체크 등)
    @ExceptionHandler(BusinessValidationException::class)
    fun handleBusiness(ex: BusinessValidationException): ResponseEntity<ApiResult<Nothing>> {
        val errors: Map<String, List<String>> = ex.errors.mapValues { listOf(it.value) }

        return ResponseEntity
            .badRequest()
            .body(ApiResult.failure(errors))
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
            .body(ApiResult.failure(errors))
    }


    // 잘못된 값
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResult<Nothing>> {
        val errors = mapOf("state" to listOf(ex.message ?: "잘못된 요청입니다."))
        return ResponseEntity
            .badRequest()
            .body(ApiResult.failure(errors))
    }

    // 잘못된 상태 예외
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(e: IllegalStateException): ResponseEntity<ApiResult<Nothing>> {
        val errors = mapOf("state" to listOf(e.message ?: "잘못된 상태입니다."))
        return ResponseEntity
            .badRequest()
            .body(ApiResult.failure(errors))
    }

    // 404 Not Found 전용
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException::class)
    fun handleResponseStatusException(e: org.springframework.web.server.ResponseStatusException): ResponseEntity<ApiResult<Nothing>> {
        return when (e.statusCode.value()) {
            404 -> {
                val errors = mapOf("not_found" to listOf(e.reason ?: "요청하신 API 또는 리소스를 찾을 수 없습니다."))
                ResponseEntity
                    .status(404)
                    .body(ApiResult.failure(errors))
            }
            else -> {
                val errors = mapOf("error" to listOf(e.reason ?: "요청 처리 중 오류가 발생했습니다."))
                ResponseEntity
                    .status(e.statusCode)
                    .body(ApiResult.failure(errors))
            }
        }
    }

    // 최상위 에러 (404 등 잡히지 않은 나머지 모든 예외 처리)
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResult<Nothing>> {
        val errors = mapOf("error" to listOf("서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."))
        e.printStackTrace()
        return ResponseEntity
            .internalServerError()
            .body(ApiResult.failure(errors))
    }

//    @ExceptionHandler(NotEnoughPointException::class)
//    fun handleNotEnoughPointException(e: NotEnoughPointException): ResponseEntity<ApiResult<>> {
//        return ResponseEntity
//            .status(HttpStatus.BAD_REQUEST)
//            .body(ApiErrorResponse("NOT_ENOUGH_POINT", e.message ?: "포인트 부족"))
//    }

    // 포인트 부족 오류
    @ExceptionHandler(NotEnoughPointException::class)
    fun handleEnoughPointException(e: NotEnoughPointException): ResponseEntity<ApiResult<Nothing>> {
        val errors = mapOf("code" to listOf("POINT"))
        return ResponseEntity
            .badRequest()
            .body(ApiResult.failure(errors))
    }


    // 이미 회원가입 된 사용자가 다시 회원가입 시도할 때
    @ExceptionHandler(AlreadyRegisteredException::class)
    fun handleAlreadyRegisteredException(e: AlreadyRegisteredException): ResponseEntity<ApiResult<Nothing>> {
        val errors = mapOf("code" to listOf("ALREADY_REGISTERED"))
        return ResponseEntity
            .badRequest()
            .body(ApiResult.failure(errors))
    }



}
