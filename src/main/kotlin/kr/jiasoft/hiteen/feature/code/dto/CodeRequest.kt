package kr.jiasoft.hiteen.feature.code.dto

import kr.jiasoft.hiteen.feature.code.domain.CodeStatus

data class CodeRequest(
    val code: String,
    val codeName: String,
    val group: String,
    val status: CodeStatus = CodeStatus.ACTIVE
)
