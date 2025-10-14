package kr.jiasoft.hiteen.feature.pin.app

import PinResponse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.common.exception.BusinessValidationException
import kr.jiasoft.hiteen.feature.level.app.ExpService
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import kr.jiasoft.hiteen.feature.pin.domain.PinUsersEntity
import kr.jiasoft.hiteen.feature.pin.dto.AllowedFriend
import kr.jiasoft.hiteen.feature.pin.dto.PinRegisterRequest
import kr.jiasoft.hiteen.feature.pin.dto.PinUpdateRequest
import kr.jiasoft.hiteen.feature.pin.infra.PinRepository
import kr.jiasoft.hiteen.feature.pin.infra.PinUsersRepository
import kr.jiasoft.hiteen.feature.push.app.PushService
import kr.jiasoft.hiteen.feature.push.domain.PushTemplate
import kr.jiasoft.hiteen.feature.push.domain.buildPushData
import kr.jiasoft.hiteen.feature.relationship.infra.FriendRepository
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import kr.jiasoft.hiteen.feature.user.infra.UserRepository
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

import kotlin.collections.associateBy

@Service
class PinService(
    private val pinRepository: PinRepository,
    private val pinUsersRepository: PinUsersRepository,
    private val userRepository: UserRepository,
    private val expService: ExpService,
    private val pushService: PushService,
    private val friendRepository: FriendRepository,
) {

    enum class VISIBILITY {
        PUBLIC, PRIVATE, FRIENDS
    }

    /** 내가 등록한 핀 목록 */
    suspend fun listMyPins(user: UserEntity): List<PinResponse> {
        val myPins = pinRepository.findAllByUserId(user.id).toList()

        // pinId → 허용된 userId 리스트
        val pinUsersMap: Map<Long, List<Long>> = pinUsersRepository.findAllByPinIdIn(myPins.map { it.id })
            .toList()
            .groupBy({ it.pinId }, { it.userId })

        val friendIds = pinUsersMap.values.flatten().distinct()
        val friends = userRepository.findAllById(friendIds).toList()
        val friendMap = friends.associateBy({ it.id }, { it })

        return myPins.map { pin ->
            val allowedFriends =
                if (pin.visibility == VISIBILITY.FRIENDS.name) {
                    pinUsersMap[pin.id]?.mapNotNull { fid ->
                        friendMap[fid]?.let { AllowedFriend(it.uid.toString(), it.nickname) }
                    } ?: emptyList()
                } else {
                    emptyList()
                }

            PinResponse.from(pin, user, allowedFriends)
        }
    }



    /**
     * 나에게 공개된 핀 목록
     * - 전체공개 (PUBLIC) -> 내주변 pin 목록
     * - 나만보기 (PRIVATE) → 등록자가 본인일 때만
     * - 일부공개 (FRIENDS)
     */
    suspend fun listVisiblePins(user: UserEntity, lat: Double, lng: Double, radius: Double): List<PinResponse> {
        val userId = user.id

        // 1. 내 주변 전체공개 핀 (반경 radius m)
        val publicPins = pinRepository.findNearbyPublicPins(
            VISIBILITY.PUBLIC.name,
            lat,
            lng,
            radius
        )

        // 2. 내 PRIVATE 핀 + 내 24시간 안 지난 FRIENDS 핀
        val myPins = pinRepository.findAllByUserId(userId)
            .filter { pin ->
                when (pin.visibility) {
                    VISIBILITY.PRIVATE.name -> true // 항상 유지
                    VISIBILITY.FRIENDS.name -> pin.createdAt.isAfter(OffsetDateTime.now().minusHours(24)) // 24시간 내만
                    else -> false
                }
            }

        // 3. 나에게 공개된 FRIENDS 핀 (24시간 제한)
        val friendPins = pinUsersRepository.findAllByUserId(userId)
            .map { it.pinId }
            .toList()
            .let { ids: List<Long> ->
                if (ids.isNotEmpty()) {
                    pinRepository.findAllById(ids)
                        .filter { it.visibility == VISIBILITY.FRIENDS.name && it.createdAt.isAfter(OffsetDateTime.now().minusHours(24)) }
                } else emptyFlow()
            }

        // 4. merge 후 collect
        val pins = merge(publicPins, myPins, friendPins).toList()

        // 작성자/친구 userId 모으기
        val userIds = pins.map { it.userId }.distinct()

        // 핀별 허용된 친구 매핑
        val pinUsers = pinUsersRepository.findAllByPinIdIn(pins.map { it.id }).toList()
        val allowedUserIds = pinUsers.groupBy({ it.pinId }, { it.userId })

        // 전체 user 조회
        val users = userRepository.findAllById(userIds + allowedUserIds.values.flatten()).toList()
        val userMap = users.associateBy({ it.id }, { it })

        return pins.map { pin ->
            val owner = userMap[pin.userId] ?: return@map PinResponse.from(pin, user)
//            val friends = allowedUserIds[pin.id]?.mapNotNull { fid ->
//                userMap[fid]?.let { AllowedFriend(it.uid.toString(), it.nickname) }
//            } ?: emptyList()
//            PinResponse.from(pin, owner, friends)
            PinResponse.from(pin, owner)
        }
    }

    suspend fun register(user: UserEntity, dto: PinRegisterRequest): PinEntity {
        // ① 핀 저장
        val pin = pinRepository.save(
            PinEntity(
                userId = user.id,
                zipcode = dto.zipcode,
                lat = dto.lat,
                lng = dto.lng,
                description = dto.description,
                visibility = dto.visibility,
                createdId = user.id,
                createdAt = OffsetDateTime.now(),
            )
        )

        // ② FRIENDS 공개 시 친구 uid → id 매핑 미리 조회
        val selectedFriendIds: List<Long> = if (dto.visibility == "FRIENDS" && !dto.friendUids.isNullOrEmpty()) {
            userRepository.findIdByUidIn(dto.friendUids)
        } else {
            emptyList()
        }

        // ③ FRIENDS 공개 시 pinUsers 저장
        if (selectedFriendIds.isNotEmpty()) {
            coroutineScope {
                selectedFriendIds.forEach { friendId ->
                    launch {
                        pinUsersRepository.save(
                            PinUsersEntity(
                                pinId = pin.id,
                                userId = friendId,
                                createdAt = OffsetDateTime.now()
                            )
                        )
                    }
                }
            }
        }

        // ④ 경험치 지급
        expService.grantExp(user.id, "PIN_REGISTER", pin.id)

        // ⑤ 푸시 알림 전송
        coroutineScope {
            launch {
                try {
                    // FRIENDS 공개일 때: 선택된 친구에게만 푸시
                    // 그 외에는 전체 친구에게 푸시
                    val friendIds =
                        if (dto.visibility == "FRIENDS" && selectedFriendIds.isNotEmpty()) {
                            selectedFriendIds
                        } else {
                            friendRepository.findAllFriendship(user.id).toList()
                        }

                    if (friendIds.isNotEmpty()) {
                        val data = PushTemplate.PIN_REGISTER.buildPushData(
                            "nickname" to user.nickname
                        )
                        pushService.sendAndSavePush(friendIds, data)
                        println("📢 ${friendIds.size}명에게 PIN_ALERT 푸시 전송 완료")
                    } else {
                        println("⚠️ 푸시 전송 대상 없음 — visibility=${dto.visibility}")
                    }
                } catch (e: Exception) {
                    println("‼️ PIN_ALERT 푸시 전송 중 오류: ${e.message}")
                }
            }
        }

        return pin
    }


    suspend fun update(user: UserEntity, dto: PinUpdateRequest): PinEntity {
        // 1. 기존 핀 조회
        val pin = pinRepository.findById(dto.id)
            ?: throw IllegalArgumentException("존재하지 않는 핀입니다.")

        // 2. 권한 체크 (작성자가 본인인지)
        if (pin.userId != user.id) {
            throw BusinessValidationException(mapOf("message" to "본인이 등록한 핀만 수정할 수 있습니다."))
        }

        // 3. 값 수정
        val updated = pin.copy(
            zipcode     = dto.zipcode     ?: pin.zipcode,
            lat         = dto.lat         ?: pin.lat,
            lng         = dto.lng         ?: pin.lng,
            description = dto.description ?: pin.description,
            visibility  = dto.visibility  ?: pin.visibility,
            updatedId   = user.id,
            updatedAt   = OffsetDateTime.now(),
        )
        val saved = pinRepository.save(updated)

        // 4. 공개 친구 수정
        when (updated.visibility) {
            VISIBILITY.FRIENDS.name -> {
                if (dto.friendUids != null) {
                    pinUsersRepository.deleteAllByPinId(pin.id)
                    if (dto.friendUids.isNotEmpty()) {
                        val users = userRepository.findAllByUidIn(dto.friendUids)
                        users.forEach { u ->
                            pinUsersRepository.save(
                                PinUsersEntity(
                                    pinId = pin.id,
                                    userId = u.id,
                                    createdAt = OffsetDateTime.now()
                                )
                            )
                        }
                    }
                }
            }
            VISIBILITY.PUBLIC.name, VISIBILITY.PRIVATE.name -> {
                pinUsersRepository.deleteAllByPinId(pin.id)
            }
        }

        return saved
    }



    /* 핀 삭제 */
    suspend fun delete(user: UserEntity, pinId: Long) {
        val pin = pinRepository.findById(pinId)
            ?: throw IllegalArgumentException("존재하지 않는 핀입니다.")

        if (pin.userId != user.id) {
            throw IllegalAccessException("본인이 등록한 핀만 삭제할 수 있습니다.")
        }

        // 친구 공개 매핑 삭제
        pinUsersRepository.deleteAllByPinId(pinId)

        // 핀 삭제
        pinRepository.deleteById(pinId)
    }



}
