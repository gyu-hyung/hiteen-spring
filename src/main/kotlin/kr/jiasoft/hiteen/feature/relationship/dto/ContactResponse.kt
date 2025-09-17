package kr.jiasoft.hiteen.feature.relationship.dto

import kr.jiasoft.hiteen.feature.user.dto.UserSummary

data class ContactResponse(
    val registeredUsers: List<UserSummary>,
    val friends: List<UserSummary>,
    val notRegisteredUsers: List<String>
)