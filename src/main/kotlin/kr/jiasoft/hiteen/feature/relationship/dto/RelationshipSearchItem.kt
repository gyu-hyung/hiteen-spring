package kr.jiasoft.hiteen.feature.relationship.dto

data class RelationshipSearchItem(
    val uid: String,
    val username: String,
    val nickname: String?,
    val relation: String? = null // null|PENDING_OUT|PENDING_IN|ACCEPTED|BLOCKED|SELF
)