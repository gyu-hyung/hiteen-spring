package kr.jiasoft.hiteen.feature.user.domain

enum class PushItemType {
    ALL,
//    FRIEND_REQUEST, FRIEND_ACCEPT, FOLLOW_REQUEST, FOLLOW_ACCEPT,
    FRIEND, FOLLOW,
    NEW_POST, PIN_ALERT, COMMENT_ALERT, CHAT_MESSAGE
}
