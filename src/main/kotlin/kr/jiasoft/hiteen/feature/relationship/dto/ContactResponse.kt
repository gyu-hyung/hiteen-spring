package kr.jiasoft.hiteen.feature.relationship.dto

import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.user.dto.UserSummary

@Schema(description = "연락처 동기화 응답")
data class ContactResponse(

    @param:Schema(
        description = "앱에 가입된 사용자 목록",
        example = "[{\"uid\":\"6f9b90d6-96ca-49de-b9c2-b123e51ca7db\",\"nickname\":\"홍길동\",\"username\":\"hong\"}]"
    )
    val registeredUsers: List<UserSummary>,

    @param:Schema(
        description = "내 친구로 등록된 사용자 목록",
        example = "[{\"uid\":\"f55db2b7-c8f3-4ebf-94c7-577bc4a3939b\",\"nickname\":\"김철수\",\"username\":\"kim\"}]"
    )
    val friends: List<UserSummary>,

    @param:Schema(
        description = "앱에 가입하지 않은 연락처 목록",
        example = "[\"01012345678\", \"01098765432\"]"
    )
    val notRegisteredUsers: List<String>
)
