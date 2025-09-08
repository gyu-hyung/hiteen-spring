package kr.jiasoft.hiteen.feature.user.dto

import kr.jiasoft.hiteen.feature.school.dto.SchoolDto
import java.time.LocalDate
import java.time.LocalDateTime

data class UserResponse(

    /* id */
//    val id: Long?,

    /* uid */
    val uid: String?,

    /* username */
    val username: String,

    /* email */
    val email: String?,

    /* nickname */
    val nickname: String?,

    /* role */
    val role: String,

    /* address */
    val address: String? = null,

    /* detail_address */
    val detailAddress: String? = null,

    /* phone */
    val phone: String? = null,

    /* mood */
    val mood: String? = null,

    /* tier */
    val tier: String? = null,

    /* asset_uid */
    val assetUid: String? = null,

    val school: SchoolDto? = null,

    val grade: String? = null,

    val gender: String? = null,

    val birthday: LocalDate? = null,

    /* created_at */
    val createdAt: LocalDateTime,

    /* updated_at */
    val updatedAt: LocalDateTime?,

    /* deleted_at */
    val deletedAt: LocalDateTime? = null,
)
