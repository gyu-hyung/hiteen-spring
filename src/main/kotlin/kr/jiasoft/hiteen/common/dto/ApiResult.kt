package kr.jiasoft.hiteen.common.dto

/*
    API 기본 응답
 */
data class ApiResult<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)