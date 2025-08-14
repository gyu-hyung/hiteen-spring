//import jakarta.annotation.PostConstruct
//import org.eclipse.paho.mqttv5.client.IMqttToken
//import org.eclipse.paho.mqttv5.client.MqttCallback
//import org.eclipse.paho.mqttv5.client.MqttClient
//import org.eclipse.paho.mqttv5.client.MqttConnectionOptions
//import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
//import org.eclipse.paho.mqttv5.common.MqttException
//import org.eclipse.paho.mqttv5.common.MqttMessage
//import org.eclipse.paho.mqttv5.common.packet.MqttProperties
//
//@PostConstruct
//fun connectAndSubscribe() {
//    val broker = "tcp://192.168.49.2:30002"
//    val clientId = "tbmq_spring"
//    val topic = "\$share/group1/location/#"
//    try {
//        val client = MqttClient(broker, clientId)
//        val options = MqttConnectionOptions().apply {
//            isAutomaticReconnect = true
//            isCleanStart = true
//            userName = "tbmq_spring"
//            password = "tbmq_spring".toByteArray() // <-- 중요!!
//        }
//        client.setCallback(object : MqttCallback {
//            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
//                println("connected to: $serverURI")
//            }
//            override fun disconnected(disconnectResponse: MqttDisconnectResponse) {
//                println("disconnected: ${disconnectResponse.reasonString}")
//            }
//            override fun deliveryComplete(token: IMqttToken) {
//                println("deliveryComplete: ${token.isComplete}")
//            }
//            override fun messageArrived(topic: String?, message: MqttMessage) {
//                println("topic: $topic")
//                println("qos: ${message.qos}")
//                println("message content: ${String(message.payload)}")
//            }
//            override fun mqttErrorOccurred(exception: MqttException) {
//                println("mqttErrorOccurred: $exception")
//            }
//            override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
//                println("authPacketArrived")
//            }
//        })
//
//        client.connect(options)
//        println("MQTT 연결 성공!")
//        client.subscribe(topic, 1)
//        println("구독 완료!")
//    } catch (e: Exception) {
//        println("오류: ${e.message}")
//        e.printStackTrace()
//    }
//}
