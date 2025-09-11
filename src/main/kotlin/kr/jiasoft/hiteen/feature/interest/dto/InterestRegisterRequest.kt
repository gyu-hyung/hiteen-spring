package kr.jiasoft.hiteen.feature.interest.dto


data class InterestRegisterRequest (
    val id : Long? = null,
    val topic: String? = null,
    val category: String? = null,
    val status: String? = null,
)