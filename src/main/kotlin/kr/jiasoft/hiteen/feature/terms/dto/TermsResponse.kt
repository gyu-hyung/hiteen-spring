package kr.jiasoft.hiteen.feature.terms.dto

import kr.jiasoft.hiteen.feature.terms.domain.TermsEntity
import java.util.UUID

data class TermsResponse(
    val uid: UUID,
    val category: String?,
    val code: String?,
    val version: String?,
    val title: String?,
    val content: String?,
    val sort: Short,
    val isRequired: Short,
    val status: Short
) {
    companion object {
        fun from(entity: TermsEntity) = TermsResponse(
            uid = entity.uid,
            category = entity.category,
            code = entity.code,
            version = entity.version,
            title = entity.title,
            content = entity.content,
            sort = entity.sort,
            isRequired = entity.isRequired,
            status = entity.status
        )

        /** 기존앱 호환용: HTML → TEXT */
        fun toPlainText(entity: TermsEntity): TermsResponse {
            val cleaned = entity.content
                ?.replace(
                    Regex(
                        "(<(script|style)[^>]*>)(.*?)(</(script|style)>)",
                        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                    ),
                    ""
                )
                ?.replace(Regex("<[^>]*>"), "")
                ?.trim()

            return from(entity).copy(content = cleaned)
        }
    }
}
