package kr.jiasoft.hiteen.feature.interest.dto


data class InterestRegisterRequest (
    val id : Long,
    val topic: String,
    val category: String,
    val status: String,
)