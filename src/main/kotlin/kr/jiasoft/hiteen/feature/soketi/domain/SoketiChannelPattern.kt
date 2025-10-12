package kr.jiasoft.hiteen.feature.soketi.domain

enum class SoketiChannelPattern(val pattern: String) {
    PRIVATE_USER("PRIVATE_USER.%s"),
    PRIVATE_USER_LOCATION("PRIVATE_USER_LOCATION.%s"),
    PRIVATE_CHAT_ROOM("PRIVATE_CHAT_ROOM.%s"),
    PRIVATE_GEO("PRIVATE_USER_GEO"),
    PRESENCE_ROOM("PRESENCE_ROOM.%s");

    fun format(vararg args: Any): String = pattern.format(*args)
}
