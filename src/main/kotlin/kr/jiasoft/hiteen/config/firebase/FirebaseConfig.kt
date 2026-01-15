package kr.jiasoft.hiteen.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File
import java.io.FileInputStream

@Configuration
class FirebaseConfig {

    @Bean
    fun firebaseApp(): FirebaseApp {
        val envPath = System.getenv("FIREBASE_CREDENTIAL_PATH")
        val inputStream = when {
            envPath != null && File(envPath).exists() -> {
                FileInputStream(envPath)
            }
//            javaClass.getResourceAsStream("/firebase/gyuhyungfcm-firebase-adminsdk-fbsvc-783efd0df8.json") != null -> {
//                javaClass.getResourceAsStream("/firebase/gyuhyungfcm-firebase-adminsdk-fbsvc-783efd0df8.json")
            javaClass.getResourceAsStream("/firebase/hi-teen-6fa22-firebase-adminsdk-pw83b-f9b51c779f.json") != null -> {
                javaClass.getResourceAsStream("/firebase/hi-teen-6fa22-firebase-adminsdk-pw83b-f9b51c779f.json")

            }
            else -> {
                throw IllegalStateException("Firebase credentials not found in either environment variable or classpath")
            }
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(inputStream))
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(app: FirebaseApp): FirebaseMessaging = FirebaseMessaging.getInstance(app)
}
