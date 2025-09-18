package kr.jiasoft.hiteen.feature.pin.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "핀 등록 요청 DTO")
data class PinRegisterRequest(

    @param:Schema(description = "우편번호", example = "12345")
    val zipcode: String?,

    @param:Schema(description = "위도", example = "37.5666")
    val lat: Double,

    @param:Schema(description = "경도", example = "127.0000")
    val lng: Double,

    @param:Schema(description = "핀 설명", example = "핀 설명@#@#@!$!~@$")
    val description: String,

//    @param:Schema(description = "")
//    val type: String?,         // 카테고리

    @param:Schema(description = "핀 공개 범위", example = "PUBLIC")
    val visibility: String,    // PUBLIC / PRIVATE / FRIENDS

    @param:Schema(description = "친구 uid 리스트", example = "[a1a8990f-2443-4492-baad-699d59b272fa, a1a8990f-2443-4492-baad-699d59b272fa, a1a8990f-2443-4492-baad-699d59b272fa]")
    val friendUids: List<UUID>? // FRIENDS일 때만 사용
)
