package kr.jiasoft.hiteen.feature.integration.mqtt.dto

data class MqttResponse(
    val userId: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long
)
