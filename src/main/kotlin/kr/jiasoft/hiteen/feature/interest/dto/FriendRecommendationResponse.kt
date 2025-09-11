package kr.jiasoft.hiteen.feature.interest.dto

import kr.jiasoft.hiteen.feature.user.domain.UserPhotosEntity
import kr.jiasoft.hiteen.feature.user.dto.UserResponse

data class FriendRecommendationResponse(
    val user: UserResponse,
    val interests: List<InterestUserResponse>,
    val photos: List<UserPhotosEntity>
)