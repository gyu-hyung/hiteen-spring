package kr.jiasoft.hiteen.config.websocket

enum class RedisChannelPattern(val pattern: String) {
    CHAT_ROOM("chat:room:%s"),
    CHAT_ROOM_MEMBERS("chat:room:%s:members"),
    USER_NOTIFY("user:%s:notify"),
    USER_LOCATION("loc:user:%s");
    fun format(vararg args: Any): String = pattern.format(*args)
}