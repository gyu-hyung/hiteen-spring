package kr.jiasoft.hiteen.feature.interest.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponse

@Schema(description = "친구 추천 응답 DTO")
data class FriendRecommendationResponse(

    @param:Schema(description = "유저 정보")
    val user: UserResponse,

    @param:Schema(description = "관심사 정보")
    val interests: List<InterestUserResponse>,

    @param:Schema(description = "사진 정보")
    val photos: List<UserPhotosEntity>
)