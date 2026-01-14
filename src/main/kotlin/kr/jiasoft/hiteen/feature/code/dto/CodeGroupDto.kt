package kr.jiasoft.hiteen.feature.code.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "코드 그룹 DTO")
data class CodeGroupDto(
    @param:Schema(description = "코드 그룹 코드", example = "EMOJI")
    val code: String,

    @param:Schema(description = "코드 그룹명", example = "이모지")
    val name: String,
)

