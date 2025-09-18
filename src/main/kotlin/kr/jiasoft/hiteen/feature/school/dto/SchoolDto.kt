package kr.jiasoft.hiteen.feature.school.dto

import io.swagger.v3.oas.annotations.media.Schema


data class SchoolDto(

    @param:Schema(description = "학교 ID", example = "1")
    val id: Long,

    @param:Schema(description = "학교 이름", example = "광주고등학교")
    val name: String
)
