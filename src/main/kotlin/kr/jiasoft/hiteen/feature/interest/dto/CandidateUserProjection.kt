package kr.jiasoft.hiteen.feature.interest.dto

data class CandidateUserProjection(
    val id: Long,
    val gender: String?,
    val grade: String?,
    val schoolId: Long?,
    val schoolType: Int?  // 1=초등, 2=중등, 3=고등, 9=특수
)
