package kr.jiasoft.hiteen.feature.level.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "경험치 액션 정의")
data class ExpActionResponse(
    @field:Schema(description = "액션 코드", example = "LOGIN")
    val actionCode: String,

    @field:Schema(description = "설명", example = "로그인")
    val description: String,

    @field:Schema(description = "지급 경험치", example = "10")
    val points: Int,

    @field:Schema(description = "일일 제한 횟수", example = "3")
    val dailyLimit: Int?,
)

