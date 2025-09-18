package kr.jiasoft.hiteen.feature.code.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.code.domain.CodeStatus

@Schema(description = "코드 등록 요청")
data class CodeRequest(

    @param:Schema(description = "코드", example = "C_001")
    val code: String,

    @param:Schema(description = "코드 이름", example = "빨강")
    val codeName: String,

    @param:Schema(description = "코드 그룹", example = "COLOR")
    val group: String,

    @param:Schema(description = "코드 상태", example = "ACTIVE")
    val status: CodeStatus = CodeStatus.ACTIVE
)
