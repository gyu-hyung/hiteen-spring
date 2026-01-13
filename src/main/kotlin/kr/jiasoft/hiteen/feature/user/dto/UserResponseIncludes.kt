package kr.jiasoft.hiteen.feature.user.dto

/**
 * UserResponse가 커지면서, 필요한 연관 도메인만 선택적으로 로드하기 위한 옵션
 */
data class UserResponseIncludes(
    val school: Boolean = false,
    val schoolClass: Boolean = false,
    val tier: Boolean = false,
    val interests: Boolean = false,
    val relationshipCounts: Boolean = false,
    val relationshipFlags: Boolean = false,
    val photos: Boolean = false,
) {
    companion object {
        /** 기본 정보만(연관 조회 X) */
        fun minimal() = UserResponseIncludes(
            school = false,
            schoolClass = false,
            tier = false,
            interests = false,
            relationshipCounts = false,
            relationshipFlags = false,
            photos = false,
        )

        /** 기존 동작과 동일(전부 조회) */
        fun full() = UserResponseIncludes()
    }
}
