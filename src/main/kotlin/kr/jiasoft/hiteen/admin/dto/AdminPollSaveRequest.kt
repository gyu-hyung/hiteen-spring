package kr.jiasoft.hiteen.admin.dto

data class AdminPollSaveRequest (
    val id: Long? = null,
    val question: String? = null,
//    val photo: String? = null,
    val colorStart: String? = null,
    val colorEnd: String? = null,
    val allowComment: Int? = null,
    val status: String? = null,
)