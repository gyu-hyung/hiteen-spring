//package kr.jiasoft.hiteen.config
//
//import org.apache.camel.builder.RouteBuilder
//import org.springframework.stereotype.Component
//
//
//@Component
//class MqttConsumers : RouteBuilder() {
//    override fun configure() {
//        from("paho-mqtt5:\$share/group1/location/#" +
//                "?brokerUrl={{mqtt.broker.uri}}" +
//                "&clientId=a1" +
//                "&userName=a1" +
//                "&password=a1" +
//                "&automaticReconnect=true" +
//                "&cleanStart=true" +
////                "&cleanStart=false" +
////                "&sessionExpiryInterval=604800" +
//                "&sessionExpiryInterval=1" +
////                "&keepAliveInterval=30" +
//                "&qos=1")
//            .routeId("a1").log("a1 <- \${body}")
//
//        from("paho-mqtt5:\$share/group1/location/#" +
//                "?brokerUrl={{mqtt.broker.uri}}" +
//                "&clientId=a2" +
//                "&userName=a2" +
//                "&password=a2" +
//                "&automaticReconnect=true" +
//                "&cleanStart=true" +
////                "&cleanStart=false" +
////                "&sessionExpiryInterval=604800" +
//                "&sessionExpiryInterval=1" +
////                "&keepAliveInterval=30" +
//                "&qos=1")
//            .routeId("a2").log("a2 <- \${body}")
//    }
//}
