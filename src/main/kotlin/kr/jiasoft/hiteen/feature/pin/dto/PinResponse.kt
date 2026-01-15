import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.pin.domain.PinEntity
import kr.jiasoft.hiteen.feature.pin.dto.AllowedFriend
import kr.jiasoft.hiteen.feature.user.domain.UserEntity
import java.time.OffsetDateTime

@Schema(description = "지도 핀 응답 DTO")
data class PinResponse(

    @param:Schema(description = "핀 ID", example = "101")
    val id: Long? = null,

    @param:Schema(description = "핀 소유자 UID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userUid: String,

    @param:Schema(description = "핀 소유자 닉네임", example = "홍길동")
    val nickname: String? = null,

    @param:Schema(description = "핀 소유자 기분", example = "졸려~~")
    val mood: String? = null,

    @param:Schema(description = "핀 소유자 프로필", example = "홍길동")
    val assetUid: String? = null,

    @param:Schema(description = "우편번호", example = "12345")
    val zipcode: String? = null,

    @param:Schema(description = "위도", example = "37.5665")
    val lat: Double? = null,

    @param:Schema(description = "경도", example = "126.9780")
    val lng: Double? = null,

    @param:Schema(description = "핀 설명", example = "우리 학교 앞 약속 장소")
    val description: String? = null,

    @param:Schema(description = "핀 공개 범위", example = "PUBLIC", allowableValues = ["PUBLIC", "PRIVATE", "FRIENDS"])
    val visibility: String? = null,

    @param:Schema(description = "허용된 친구 목록 (FRIENDS 공개 범위일 경우만 포함)")
    val allowedFriends: List<AllowedFriend> = emptyList(),

    @param:Schema(description = "생성 일시", example = "2025.09.18 10:15:00")
    @field:JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
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
            mood = owner.mood,
            assetUid = owner.assetUid.toString(),
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
