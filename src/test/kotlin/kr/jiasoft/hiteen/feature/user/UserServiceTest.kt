package kr.jiasoft.hiteen.feature.user

import kotlinx.coroutines.runBlocking
import kr.jiasoft.hiteen.feature.user.dto.UserRegisterForm
import kr.jiasoft.hiteen.feature.user.dto.UserUpdateForm
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class UserServiceTest {

    private val userRepository: UserRepository = mock()
//    private val encoder = BCryptPasswordEncoder()
    private val encoder: PasswordEncoder = mock()

    private val userService = UserService(userRepository, encoder)


    @Test
    fun `같은 비밀번호는 항상 matches true`() {
        val encoder = BCryptPasswordEncoder()  // 진짜 객체!
        val raw = "1234"
        val hash = encoder.encode(raw)
        println("bcrypt 해시: $hash")
        assertTrue(encoder.matches(raw, hash))
    }

    @Test
    fun `다른 비밀번호는 matches false`() {
        val hash = encoder.encode("1234")

        assertFalse(encoder.matches("wrongpw", hash))
    }

    @Test
    fun `이미 만들어진 해시도 matches 정상동작`() {
        val encoder = BCryptPasswordEncoder()
        val raw = "1234"
        val hash = encoder.encode(raw)
        println("bcrypt 해시: $hash")
        assertTrue(encoder.matches(raw, hash)) // 무조건 true
    }


    @Test
    fun `비밀번호 비교`() {
        val encoder = BCryptPasswordEncoder()  // 진짜 객체!
        val raw = "1234"
        val hash = encoder.encode(raw)
        println("bcrypt 해시: $hash")
        assertTrue(encoder.matches(raw, hash))
    }

//    @Test
//    fun `회원가입 - 비밀번호 인코딩 및 저장`() {
//        runBlocking {
//            // given
//            val registerForm = UserRegisterForm("user1", "user1@test.com", "닉", "pw1234")
//            whenever(encoder.encode("pw1234")).thenReturn("encoded_pw")
//            val entity = registerForm.toEntity("encoded_pw")
//            whenever(userRepository.save(entity)).thenReturn(entity)
//
//            // when
//            val result = userService.register(registerForm)
//
//            // then
//            assertEquals("user1", result.username)
//            assertEquals("닉", result.nickname)
////            assertEquals("encoded_pw", result.password)
//            verify(userRepository).save(entity)
//        }
//    }
//
//    @Test
//    fun `회원정보수정 - 닉네임 이메일 변경`() {
//        runBlocking {
//            // given
//            val user = UserEntity(id=1L, username="user1", email="old@mail.com", nickname="old", password="pw", role="USER", createdAt = LocalDateTime.now())
//            val form = UserUpdateForm(nickname="새닉", email="new@mail.com")
//            whenever(userRepository.findById(1L)).thenReturn(user)
//            val updated = user.copy(nickname="새닉", email="new@mail.com")
//            whenever(userRepository.save(updated)).thenReturn(updated)
//
//            // when
//            val result = userService.updateUser(user, form)
//
//            // then
//            assertEquals("새닉", result.nickname)
//            assertEquals("new@mail.com", result.email)
//            verify(userRepository).save(updated)
//        }
//    }

}