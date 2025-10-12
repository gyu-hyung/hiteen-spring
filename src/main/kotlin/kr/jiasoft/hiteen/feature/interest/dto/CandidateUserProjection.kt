package kr.jiasoft.hiteen.feature.interest.dto

data class CandidateUserProjection(
    val id: Long,
    val gender: String?,
    val grade: String?,
    val schoolId: Long?
)
