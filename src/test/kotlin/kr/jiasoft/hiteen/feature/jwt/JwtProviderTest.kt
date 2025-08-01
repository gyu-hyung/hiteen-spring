package kr.jiasoft.hiteen.feature.jwt

import io.jsonwebtoken.io.Encoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test

class JwtProviderTest {


    @Test
    fun generateJWTs(){
        val key = Keys.hmacShaKeyFor("ac0da6c32199d5d4829ca62b05f2a353ab926e2855de718e28286ca64bc2f9df".toByteArray())
        println(Encoders.BASE64.encode(key.encoded))
    }

}