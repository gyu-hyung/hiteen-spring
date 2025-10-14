package kr.jiasoft.hiteen.feature.user.app

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kr.jiasoft.hiteen.feature.user.domain.UserDetailEntity
import kr.jiasoft.hiteen.feature.user.dto.UserDetailRequest
import kr.jiasoft.hiteen.feature.user.infra.UserDetailRepository
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("UserDetailService 단위 테스트")
class UserDetailServiceTest {

    private lateinit var userDetailRepository: UserDetailRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: UserDetailService
    private lateinit var objectMapper: ObjectMapper

    private val userUid = UUID.randomUUID()
    private val userId = 1L

    @BeforeEach
    fun setup() {
        userDetailRepository = mockk()
        userRepository = mockk()
        objectMapper = mockk()
        service = UserDetailService(userDetailRepository, userRepository, objectMapper)
    }

    @Test
    @DisplayName("getUserDetail - 존재하는 유저 디테일 조회 시 UserDetailResponse 반환")
    fun testGetUserDetail() = runTest {
        // given
        val entity = UserDetailEntity(
            userId = userId,
            deviceId = "device-123",
            deviceOs = "Android",
            pushItems = "[\"CHAT_MESSAGE\",\"FOLLOW\"]"
        )
        coEvery { userDetailRepository.findByUid(userUid) } returns entity

        // when
        val result = service.getUserDetail(userUid)

        // then
        assertNotNull(result)
        assertEquals("device-123", result?.deviceId)
        assertEquals(listOf("CHAT_MESSAGE", "FOLLOW"), result?.pushItems)
        coVerify(exactly = 1) { userDetailRepository.findByUid(userUid) }
    }

    @Test
    @DisplayName("getUserDetail - 존재하지 않을 경우 null 반환")
    fun testGetUserDetailNull() = runTest {
        coEvery { userDetailRepository.findByUid(userUid) } returns null

        val result = service.getUserDetail(userUid)

        assertNull(result)
        coVerify(exactly = 1) { userDetailRepository.findByUid(userUid) }
    }

    @Test
    @DisplayName("upsertUserDetail - 신규 유저일 경우 insert 수행")
    fun testUpsertInsert() = runTest {
        // given
        val request = UserDetailRequest(
            userUid = userUid,
            pushItems = listOf("FOLLOW", "CHAT_MESSAGE"),
            deviceOs = "iOS"
        )
        coEvery { userRepository.findIdByUid(userUid) } returns userId
        coEvery { userDetailRepository.findByUid(userUid) } returns null
        coEvery { userDetailRepository.save(any()) } answers { firstArg() }

        // when
        val result = service.upsertUserDetail(userUid, request)

        // then
        assertEquals("iOS", result.deviceOs)
        assertEquals(listOf("FOLLOW", "CHAT_MESSAGE"), result.pushItems)
        coVerifyOrder {
            userRepository.findIdByUid(userUid)
            userDetailRepository.findByUid(userUid)
            userDetailRepository.save(any())
        }
    }

    @Test
    @DisplayName("upsertUserDetail - 기존 유저 디테일이 있을 경우 update 수행")
    fun testUpsertUpdate() = runTest {
        val existing = UserDetailEntity(
            userId = userId,
            deviceId = "oldDevice",
            deviceOs = "Android",
            pushItems = "[\"FOLLOW\"]"
        )
        val req = UserDetailRequest(
            userUid = userUid,
            deviceId = "newDevice",
            pushItems = listOf("FOLLOW", "CHAT_MESSAGE")
        )

        coEvery { userRepository.findIdByUid(userUid) } returns userId
        coEvery { userDetailRepository.findByUid(userUid) } returns existing
        coEvery { userDetailRepository.save(any()) } answers { firstArg() }

        // when
        val result = service.upsertUserDetail(userUid, req)

        // then
        assertEquals("newDevice", result.deviceId)
        assertEquals(listOf("FOLLOW", "CHAT_MESSAGE"), result.pushItems)
        coVerifySequence {
            userRepository.findIdByUid(userUid)
            userDetailRepository.findByUid(userUid)
            userDetailRepository.save(any())
        }
    }

}
