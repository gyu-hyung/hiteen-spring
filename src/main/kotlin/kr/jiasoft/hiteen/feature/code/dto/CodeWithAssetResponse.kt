package kr.jiasoft.hiteen.feature.code.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.code.domain.CodeStatus

@Schema(description = "코드 응답 DTO")
data class CodeWithAssetResponse(

    @param:Schema(description = "코드 ID", example = "1")
    val id: Long,

    @param:Schema(description = "코드", example = "E_001")
    val code: String,        // "E_001"

    @param:Schema(description = "코드 이름", example = "웃는 얼굴")
    val name: String,        // "웃는 얼굴" (codeName)

    @param:Schema(description = "코드 상태", example = "ACTIVE")
    val status: CodeStatus,      // "ACTIVE"

    @param:Schema(description = "첨부파일 UUID", example = "a1a8990f-2443-4492-baad-699d59b272fa")
    val url: String?         // 첨부파일이 있을 경우
)
