package kr.jiasoft.hiteen.feature.pin.app

import PinResponse
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import kr.jiasoft.hiteen.feature.pin.domain.PinUsersEntity
import kr.jiasoft.hiteen.feature.pin.dto.AllowedFriend
import kr.jiasoft.hiteen.feature.pin.dto.PinRegisterRequest
import kr.jiasoft.hiteen.feature.pin.dto.PinUpdateRequest
import kr.jiasoft.hiteen.feature.pin.infra.PinRepository
import kr.jiasoft.hiteen.feature.pin.infra.PinUsersRepository
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
) {

    /** 내가 등록한 핀 목록 */
    suspend fun listMyPins(user: UserEntity): List<PinResponse> {
        val myPins = pinRepository.findAllByUserId(user.id!!).toList()

        // pinId → 허용된 userId 리스트
        val pinUsersMap: Map<Long, List<Long>> = pinUsersRepository.findAllByPinIdIn(myPins.mapNotNull { it.id })
            .toList()
            .groupBy({ it.pinId }, { it.userId })

        // userId → UserEntity 매핑
        val friendIds = pinUsersMap.values.flatten().distinct()
        val friends = userRepository.findAllById(friendIds).toList()
        val friendMap = friends.associateBy({ it.id }, { it })

        return myPins.map { pin ->
            val allowedFriends = pinUsersMap[pin.id]?.mapNotNull { fid ->
                friendMap[fid]?.let { AllowedFriend(it.uid.toString(), it.nickname) }
            } ?: emptyList()

            PinResponse.from(pin, user, allowedFriends)
        }
    }


    /**
     * 나에게 공개된 핀 목록
     * - 전체공개 (PUBLIC) -> TODO 내주변 pin 목록
     * - 나만보기 (PRIVATE) → 등록자가 본인일 때만
     * - 일부공개 (FRIENDS)
     */
    suspend fun listVisiblePins(user: UserEntity): List<PinResponse> {
        val userId = user.id!!

        // 1. 전체 공개 핀 TODO 근처 핀만 조회로 변경해야함
        val publicPins = pinRepository.findAllByVisibilityOrderByIdDesc("PUBLIC")

        // 2. 내 PRIVATE 핀
        val myPrivatePins = pinRepository.findAllByUserId(userId)
            .filter { it.visibility == "PRIVATE" }

        // 3. 나에게 공개된 FRIENDS 핀
        val friendPins = pinUsersRepository.findAllByUserId(userId)
            .map { it.pinId }
            .toList()
            .let { ids: List<Long> ->
                if (ids.isNotEmpty()) pinRepository.findAllById(ids)
                else emptyFlow()
            }

        // 4. merge 후 collect
        val pins = merge(publicPins, myPrivatePins, friendPins).toList()

        // 작성자/친구 userId 모으기
        val userIds = pins.map { it.userId }.distinct()

        // 핀별 허용된 친구 매핑
        val pinUsers = pinUsersRepository.findAllByPinIdIn(pins.mapNotNull { it.id }).toList()
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
        val pin = pinRepository.save(
            PinEntity(
                userId = user.id!!,
                zipcode = dto.zipcode,
                lat = dto.lat,
                lng = dto.lng,
                description = dto.description,
                visibility = dto.visibility,
                createdId = user.id
            )
        )


        //TODO 해당 uid 가 친구가 맞는지?
        if (dto.visibility == "FRIENDS" && !dto.friendUids.isNullOrEmpty()) {
            // uid -> id 매핑을 한번에 조회
            val users = userRepository.findAllByUidIn(dto.friendUids)
            coroutineScope {
                users.forEach { u ->
                    launch {
                        pinUsersRepository.save(
                            PinUsersEntity(pinId = pin.id!!, userId = u.id!!)
                        )
                    }
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
            throw IllegalAccessException("본인이 등록한 핀만 수정할 수 있습니다.")
        }

        // 3. 값 수정
        val updated = pin.copy(
            zipcode = dto.zipcode,
            lat = dto.lat,
            lng = dto.lng,
            description = dto.description,
            visibility = dto.visibility,
            updatedId = user.id,
            updatedAt = OffsetDateTime.now(),
        )
        val saved = pinRepository.save(updated)

        // 4. 공개 친구 수정
        if (dto.visibility == "FRIENDS") {
            if (dto.friendUids != null) {
                pinUsersRepository.deleteAllByPinId(pin.id!!)
                if (dto.friendUids.isNotEmpty()) {
                    val users = userRepository.findAllByUidIn(dto.friendUids)
                    users.forEach { u ->
                        pinUsersRepository.save(
                            PinUsersEntity(pinId = pin.id, userId = u.id!!)
                        )
                    }
                }
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
