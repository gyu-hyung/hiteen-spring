package kr.jiasoft.hiteen.feature.relationship.domain

enum class RelationType {
    FRIEND, FOLLOW
}

enum class RelationAction {
    REQUEST, ACCEPT, DENIED, CANCEL, REMOVE, FOLLOWER_REMOVE
}
