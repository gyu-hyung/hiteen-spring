package kr.jiasoft.hiteen.feature.user.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import kr.jiasoft.hiteen.feature.user.domain.PushItemType
import java.util.UUID

data class UserDetailRequest(
    @field:Schema(description = "사용자 UID (외부 공개 식별자)", example = "550e8400-e29b-41d4-a716-446655440000")
    val userUid: UUID? = null,

    @field:Schema(description = "사용자 ID (내부 PK)", example = "1")
    var userId: Long? = null,

    @field:Schema(description = "디바이스 고유 ID", example = "A1234567890XYZ")
    val deviceId: String? = null,

    @field:Schema(description = "디바이스 OS", example = "iOS")
    val deviceOs: String? = null,

    @field:Schema(description = "디바이스 OS 버전", example = "17.2")
    val deviceVersion: String? = null,

    @field:Schema(description = "디바이스 상세 모델명", example = "iPhone 15 Pro")
    val deviceDetail: String? = null,

    @field:Schema(description = "푸시 알림용 디바이스 토큰", example = "fcm_device_token_12345")
    val deviceToken: String? = null,

    @field:Schema(description = "위치 알림용 토큰", example = "location_token_abc123")
    val locationToken: String? = null,

    @field:Schema(description = "AQNS 서비스 토큰", example = "aqns_token_xyz987")
    val aqnsToken: String? = null,

    @field:Schema(description = "외부 API 접근 토큰", example = "api_token_123456")
    val apiToken: String? = null,

    @field:Schema(description = "서비스 약관 동의 여부", example = "Y")
    val agreeService: String? = null,

    @field:Schema(description = "개인정보 처리 방침 동의 여부", example = "Y")
    val agreePrivacy: String? = null,

    @field:Schema(description = "금융 약관 동의 여부", example = "N")
    val agreeFinance: String? = null,

    @field:Schema(description = "마케팅 수신 동의 여부", example = "Y")
    val agreeMarketing: String? = null,

    @field:Schema(description = "서비스 푸시 알림 수신 여부", example = "Y")
    val pushService: String? = null,

    @field:Schema(description = "마케팅 푸시 알림 수신 여부", example = "N")
    val pushMarketing: String? = null,

    @field:Schema(description = "푸시 수신 항목", example = """
        ["ALL","FRIEND_REQUEST","FRIEND_ACCEPT","FOLLOW","NEW_POST","PIN_ALERT","COMMENT_ALERT","CHAT_MESSAGE"]
    """)
    val pushItems: List<PushItemType>? = null,

    @field:Schema(description = "관리자 메모", example = "VIP 고객, 빠른 응대 필요")
    val memo: String? = null
)

data class UserDetailResponse(
    @JsonIgnore
    @field:Schema(description = "사용자 ID (내부 PK)", example = "1")
    val userId: Long,

    @field:Schema(description = "디바이스 고유 ID", example = "A1234567890XYZ")
    val deviceId: String?,

    @field:Schema(description = "디바이스 OS", example = "Android")
    val deviceOs: String?,

    @field:Schema(description = "디바이스 OS 버전", example = "14")
    val deviceVersion: String?,

    @field:Schema(description = "디바이스 상세 모델명", example = "Galaxy S24 Ultra")
    val deviceDetail: String?,

    @field:Schema(description = "푸시 알림용 디바이스 토큰", example = "fcm_device_token_67890")
    val deviceToken: String?,

    @field:Schema(description = "위치 알림용 토큰", example = "location_token_zzz111")
    val locationToken: String?,

    @field:Schema(description = "AQNS 서비스 토큰", example = "aqns_token_def456")
    val aqnsToken: String?,

    @field:Schema(description = "외부 API 접근 토큰", example = "api_token_abcdef")
    val apiToken: String?,

    @field:Schema(description = "서비스 약관 동의 여부", example = "Y")
    val agreeService: String?,

    @field:Schema(description = "개인정보 처리 방침 동의 여부", example = "Y")
    val agreePrivacy: String?,

    @field:Schema(description = "금융 약관 동의 여부", example = "N")
    val agreeFinance: String?,

    @field:Schema(description = "마케팅 수신 동의 여부", example = "N")
    val agreeMarketing: String?,

    @field:Schema(description = "서비스 푸시 알림 수신 여부", example = "Y")
    val pushService: String?,

    @field:Schema(description = "마케팅 푸시 알림 수신 여부", example = "N")
    val pushMarketing: String?,

    @field:Schema(description = "푸시 수신 항목", example = """
        ["ALL","FRIEND_REQUEST","FRIEND_ACCEPT","FOLLOW_REQUEST","FOLLOW_RESPONSE","NEW_POST","PIN_ALERT","COMMENT_ALERT","CHAT_MESSAGE"]
    """)
    val pushItems: List<String>? = null,

    @field:Schema(description = "관리자 메모", example = "앱 업데이트 권장")
    val memo: String?
)
