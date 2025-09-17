package kr.jiasoft.hiteen.feature.code.dto

import kr.jiasoft.hiteen.feature.code.domain.CodeStatus

data class CodeWithAssetResponse(
    val id: Long,
    val code: String,        // "E_001"
    val name: String,        // "웃는 얼굴" (codeName)
    val status: CodeStatus,      // "ACTIVE"
    val url: String?         // 첨부파일이 있을 경우
)
