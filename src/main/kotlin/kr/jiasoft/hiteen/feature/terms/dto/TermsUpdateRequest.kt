package kr.jiasoft.hiteen.feature.terms.dto

data class TermsUpdateRequest(
    val category: String?,
    val code: String?,
    val version: String?,
    val title: String?,
    val content: String?,
    val sort: Short = 0,
    val isRequired: Short = 0,
    val status: Short = 1,
    val updatedId: Long,
)

