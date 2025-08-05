package kr.jiasoft.hiteen.feature.mqtt

data class MqttResponse(
    val userId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)
