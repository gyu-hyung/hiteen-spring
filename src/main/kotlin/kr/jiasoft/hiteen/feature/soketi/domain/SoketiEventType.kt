package kr.jiasoft.hiteen.feature.soketi.domain

enum class SoketiEventType(val value: String) {
    MESSAGE_CREATED("MESSAGE_CREATED"),
    ROOM_UPDATED("ROOM_UPDATED"),
    LOCATION("LOCATION"),
    TYPING("TYPING"),
    READ_RECEIPT("READ_RECEIPT"),;
}
