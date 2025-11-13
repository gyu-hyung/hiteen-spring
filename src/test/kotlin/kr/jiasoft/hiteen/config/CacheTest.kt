package kr.jiasoft.hiteen.config

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.user.app.UserService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID
import java.util.concurrent.TimeUnit

@SpringBootTest
class CacheTest {

    @Autowired
    lateinit var userService: UserService


//        6e330bdc-3062-4a14-80f2-a46e04278c5c
//        ade41de8-4276-4fb7-9473-cc69cf9e451f
    @Test
    fun `testCaffeineAsyncCache`(){
        runBlocking {

            println("▶ 첫 번째 호출 (캐시 미스)")
            val result1 = userService.findUserResponse(UUID.fromString("6e330bdc-3062-4a14-80f2-a46e04278c5c"))
            println("결과: $result1\n")

            println("▶ 두 번째 호출 (캐시 히트 예상)")
            val result2 = userService.findUserResponse(UUID.fromString("6e330bdc-3062-4a14-80f2-a46e04278c5c"))
            println("결과: $result2\n")

            val result3 = userService.findUserResponse(UUID.fromString("6e330bdc-3062-4a14-80f2-a46e04278c5c"))
            println("결과: $result3\n")

            val result4 = userService.findUserResponse(UUID.fromString("6e330bdc-3062-4a14-80f2-a46e04278c5c"))
            println("결과: $result4\n")

            val result5 = userService.findUserResponse(UUID.fromString("6e330bdc-3062-4a14-80f2-a46e04278c5c"))
            println("결과: $result5\n")

            TimeUnit.SECONDS.sleep(2)

//            println("▶ 다른 사용자 호출 (캐시 미스)")
//            val result3 = userService.findUserResponse(UUID.fromString("ade41de8-4276-4fb7-9473-cc69cf9e451f"))
//            println("결과: $result3\n")

        }
    }



}