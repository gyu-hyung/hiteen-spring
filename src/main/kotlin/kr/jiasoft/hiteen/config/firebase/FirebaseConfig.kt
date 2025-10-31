package kr.jiasoft.hiteen.config.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FirebaseConfig {

    @Bean
    fun firebaseApp(): FirebaseApp {
        val envPath = System.getenv("FIREBASE_CREDENTIAL_PATH")
        val inputStream = if (envPath != null) {
            FileInputStream(envPath)
        } else {
            // ✅ 로컬 환경일 경우 resources 폴더에서 로드
            javaClass.getResourceAsStream("/firebase/gyuhyungfcm-firebase-adminsdk-fbsvc-783efd0df8.json")
                ?: throw IllegalStateException("Firebase credentials not found")
        }

//        val inputStream = javaClass.getResourceAsStream("/firebase/gyuhyungfcm-firebase-adminsdk-fbsvc-783efd0df8.json")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(inputStream))
            .build()

        return if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
    }

    @Bean
    fun firebaseMessaging(app: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(app)
}
