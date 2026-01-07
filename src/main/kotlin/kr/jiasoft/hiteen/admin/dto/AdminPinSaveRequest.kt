package kr.jiasoft.hiteen.admin.dto

data class AdminPinSaveRequest (
    val id: Long? = null,

    val userId: Long? = null,
    val zipcode: String? = null,
    val lat: Double?,
    val lng: Double?,
    val description: String?,
    val visibility: String?,
)