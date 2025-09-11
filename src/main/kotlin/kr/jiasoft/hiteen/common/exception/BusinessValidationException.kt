package kr.jiasoft.hiteen.common.exception

class BusinessValidationException(
    val errors: Map<String, String>
) : RuntimeException("validation failed")
