package kr.jiasoft.hiteen.feature.user

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class UserServiceTest {


    private val encoder = BCryptPasswordEncoder()

    @Test
    fun `비밀번호 비교`() {
        val raw = "1234"
        val hash = encoder.encode(raw)
        println("bcrypt 해시: $hash")
        assertTrue(encoder.matches(raw, hash))
    }

}