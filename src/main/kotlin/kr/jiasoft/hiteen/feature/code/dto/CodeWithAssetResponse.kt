package kr.jiasoft.hiteen.feature.code.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.code.domain.CodeEntity
import kr.jiasoft.hiteen.feature.code.domain.CodeStatus
import java.util.UUID

@Schema(description = "코드 응답 DTO")
data class CodeWithAssetResponse(

    @param:Schema(description = "코드 ID", example = "1")
    val id: Long,

    @param:Schema(description = "코드", example = "E_001")
    val code: String,        // "E_001"

    @param:Schema(description = "코드 이름", example = "웃는 얼굴")
    val codeName: String,        // "웃는 얼굴" (codeName)

    @param:Schema(description = "코드 그룹", example = "EMOJI")
    val codeGroup: String,

    @param:Schema(description = "코드 그룹명", example = "이모지")
    val codeGroupName: String,

    @param:Schema(description = "코드 상태", example = "ACTIVE")
    val status: CodeStatus,      // "ACTIVE"

    @param:Schema(description = "첨부파일 UUID", example = "a1a8990f-2443-4492-baad-699d59b272fa")
    val assetUid: UUID?,         // 첨부파일이 있을 경우

    @param:Schema(description = "컬럼 1")
    val col1: String?,

    @param:Schema(description = "컬럼 2")
    val col2: String?,

    @param:Schema(description = "컬럼 3")
    val col3: String?,

    ) {
    companion object {
        fun from(entity: CodeEntity): CodeWithAssetResponse {
            return CodeWithAssetResponse(
                id = entity.id,
                code = entity.code,
                codeName = entity.codeName,
                codeGroup = entity.codeGroup,
                codeGroupName = entity.codeGroupName,
                status = entity.status,
                assetUid = entity.assetUid,
                col1 = entity.col1,
                col2 = entity.col2,
                col3 = entity.col3,
            )
        }
    }
}

