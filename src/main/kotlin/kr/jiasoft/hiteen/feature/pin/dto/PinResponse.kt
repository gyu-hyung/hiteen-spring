import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import kr.jiasoft.hiteen.feature.pin.dto.AllowedFriend
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import java.time.OffsetDateTime



data class PinResponse(
    val id: Long? = null,
    val userUid: String,
    val nickname: String? = null,
    val zipcode: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val description: String? = null,
    val visibility: String? = null,
    val allowedFriends: List<AllowedFriend> = emptyList(),
    val createdAt: OffsetDateTime? = null,
) {
    companion object {
        fun from(
            entity: PinEntity,
            owner: UserEntity,
            allowedFriends: List<AllowedFriend> = emptyList()
        ): PinResponse = PinResponse(
            id = entity.id,
            userUid = owner.uid.toString(),
            nickname = owner.nickname,
            zipcode = entity.zipcode,
            lat = entity.lat,
            lng = entity.lng,
            description = entity.description,
            visibility = entity.visibility,
            allowedFriends = allowedFriends,
            createdAt = entity.createdAt,
        )
    }
}
