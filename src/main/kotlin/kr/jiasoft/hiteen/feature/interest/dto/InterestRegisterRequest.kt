package kr.jiasoft.hiteen.feature.interest.dto

import io.swagger.v3.oas.annotations.media.Schema


@Schema(description = "관심사 등록 요청 DTO")
data class InterestRegisterRequest (

    @param:Schema(description = "관심사 ID", example = "1")
    val id : Long,

    @param:Schema(description = "관심사", example = "운동")
    val topic: String,

    @param:Schema(description = "카테고리", example = "운동")
    val category: String,

    @param:Schema(description = "상태", example = "Y")
    val status: String,
)